#!/usr/bin/env python3
"""
Bukkit/Spigot/Paper eklenti jar'ı -> Gradle projesi kaynak çıkarıcı.

Verilen jar içinden:
  1. --project-prefix altındaki (ve --exclude-prefix dışındaki) sınıfları Vineflower ile decompile eder,
  2. plugin.yml, config.yml, messages.yml gibi proje resource'larını src/main/resources'a çıkarır,
  3. gömülü (shade edilmiş / relocate edilmiş) kütüphaneleri libs/bundled-libs.jar olarak ayırır,
  4. Vineflower'ın bozduğu kalıpları (invokedynamic string birleştirme, boolean/int sayaç,
     ham koleksiyonlu for-each) düzeltir,
  5. DECOMPILE_REPORT.md yazar.

Kullanım:
  python3 tools/decompile.py --jar original/AxVaults.jar --root . \
      --project-prefix com/artillexstudios/axvaults/ \
      --exclude-prefix com/artillexstudios/axvaults/libs/ [--force]

--split-only: Vineflower'ı çalıştırmaz, sadece resource ve libs ayrıştırmasını yapar (ağ gerekmez).
"""
import argparse
import logging
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

LOG = logging.getLogger("decompile")

VINEFLOWER_VERSION = "1.10.1"
LIBS_JAR_NAME = "bundled-libs.jar"
SKIP_FILES = {"META-INF/MANIFEST.MF", "META-INF/INDEX.LIST"}
SIGNATURE_RE = re.compile(r"^META-INF/[^/]+\.(SF|RSA|DSA|EC)$")
LICENSE_RE = re.compile(r"(?i)^(license|licence|notice|copying)[^/]*$")
DEFAULT_LIB_RESOURCE_RE = r"^lamp.*\.properties$"
FAIL_MARKERS = ("Couldn't be decompiled", "$VF: Unable to", "Method decompilation error")

INDY_CONCAT = re.compile(
    r'StringConcatFactory\.makeConcatWithConstants<((?:"(?:[^"\\]|\\.)*"\s*,?\s*)+)>\('
)
STR_LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')
RECIPE_MARKERS = re.compile(r"(\\u0001|\\u0002|\x01|\x02)")

DUCK_CAST = re.compile(r"\(\((I[A-Z]\w*)\)([A-Za-z_][\w.]*)\)")
GETVALUE_COND = re.compile(r"((?:[A-Za-z_]\w*(?:\([^()]*\))?\.)+getValue\(\)) \?")
BOOL_ASSIGN = re.compile(r"\b([A-Za-z_]\w*) = \(boolean\)(\d+)")
INT_USE = r"(?:\b{n}\s*(?:\+\+|--|\+=|-=)|(?:\+\+|--){n}\b)"


def _split_call_args(text: str, start: int):
    """start: '(' sonrası indeks. (argümanlar, kapanış ')' indeksi) döner, bulamazsa None."""
    depth = 0
    i = start
    cur = start
    args = []
    n = len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            quote = c
            i += 1
            while i < n and text[i] != quote:
                if text[i] == "\\":
                    i += 1
                i += 1
        elif c in "([{":
            depth += 1
        elif c in ")]}":
            if depth == 0:
                args.append(text[cur:i].strip())
                return args, i
            depth -= 1
        elif c == "," and depth == 0:
            args.append(text[cur:i].strip())
            cur = i + 1
        i += 1
    return None


def fix_indy_concat(text: str) -> str:
    """Vineflower'ın çıkaramadığı invokedynamic string birleştirmeyi düz Java'ya çevirir."""
    out = []
    pos = 0
    while True:
        m = INDY_CONCAT.search(text, pos)
        if not m:
            out.append(text[pos:])
            break
        parsed = _split_call_args(text, m.end())
        literals = STR_LITERAL.findall(m.group(1))
        if parsed is None or len(literals) < 2:
            out.append(text[pos:m.end()])
            pos = m.end()
            continue
        args, close = parsed
        if args == [""]:
            args = []
        recipe = literals[1]
        constants = literals[2:]
        pieces = []
        a_i = c_i = 0
        ok = True
        for frag in RECIPE_MARKERS.split(recipe):
            if frag in ("\\u0001", "\x01"):
                if a_i >= len(args):
                    ok = False
                    break
                pieces.append("(" + args[a_i] + ")")
                a_i += 1
            elif frag in ("\\u0002", "\x02"):
                if c_i >= len(constants):
                    ok = False
                    break
                pieces.append('"' + constants[c_i] + '"')
                c_i += 1
            elif frag:
                pieces.append('"' + frag + '"')
        if not ok or a_i != len(args):
            out.append(text[pos:m.end()])
            pos = m.end()
            continue
        out.append(text[pos:m.start()])
        out.append('("" + ' + " + ".join(pieces) + ")" if pieces else '""')
        pos = close + 1
    return "".join(out)


def fix_bool_int(text: str) -> str:
    """Vineflower bazen int sayacı boolean sanıyor: (boolean)0 + x++ kalıbını düzeltir."""
    names = {m.group(1) for m in BOOL_ASSIGN.finditer(text)}
    for name in names:
        if not re.search(INT_USE.format(n=re.escape(name)), text):
            continue
        n = re.escape(name)
        text = re.sub(rf"\bboolean {n}\b", f"int {name}", text)
        text = re.sub(rf"\b{n} = \(boolean\)(\d+)", rf"{name} = \1", text)
        text = re.sub(rf"(\bint {n} = )false\b", r"\g<1>0", text)
        text = re.sub(rf"(\bint {n} = )true\b", r"\g<1>1", text)
        text = re.sub(rf"(\b{n} = )false;", r"\g<1>0;", text)
        text = re.sub(rf"(\b{n} = )true;", r"\g<1>1;", text)
    return text


RAW_FOREACH = re.compile(
    r"(for \([^:;{}]+? \w+ : new (?:ArrayList|LinkedList|HashSet|LinkedHashSet|ArrayDeque|TreeSet))\("
)


def fix_raw_foreach(text: str) -> str:
    """for (T x : new ArrayList(c)) -> new ArrayList<>(c): ham koleksiyon Object döndürür."""
    return RAW_FOREACH.sub(lambda m: m.group(1) + "<>(", text)


def fix_duck_casts(text: str) -> str:
    """((IFoo)x) -> ((IFoo)(Object)x). Sadece --legacy-fixes ile (mixin duck interface kalıbı)."""
    return DUCK_CAST.sub(lambda m: f"(({m.group(1)})(Object){m.group(2)})", text)


def fix_getvalue_cond(text: str) -> str:
    """a.getValue() ? x : y -> ((Boolean) a.getValue()) ? x : y. Sadece --legacy-fixes ile."""
    return GETVALUE_COND.sub(lambda m: f"((Boolean) {m.group(1)}) ?", text)


def download(url: str, dest: Path) -> None:
    LOG.info("indiriliyor: %s", url)
    req = urllib.request.Request(url, headers={"User-Agent": "plugin-bootstrap"})
    with urllib.request.urlopen(req, timeout=120) as resp, open(dest, "wb") as fh:
        shutil.copyfileobj(resp, fh)


def read_plugin_info(zin: zipfile.ZipFile) -> dict:
    names = set(zin.namelist())
    if "plugin.yml" not in names and "paper-plugin.yml" not in names:
        hint = ""
        if "fabric.mod.json" in names or "META-INF/mods.toml" in names or "META-INF/neoforge.mods.toml" in names:
            hint = " (bu bir Fabric/Forge modu görünüyor, bu script eklenti içindir)"
        raise SystemExit("HATA: jar içinde plugin.yml / paper-plugin.yml yok" + hint)
    src = "plugin.yml" if "plugin.yml" in names else "paper-plugin.yml"
    text = zin.read(src).decode("utf-8", errors="replace")
    info = {"file": src}
    for key in ("name", "main", "version", "api-version"):
        m = re.search(rf"(?m)^{re.escape(key)}:\s*['\"]?([^'\"\r\n]+?)['\"]?\s*$", text)
        if m:
            info[key] = m.group(1).strip()
    if "name" not in info or "main" not in info:
        raise SystemExit(f"HATA: {src} içinde name/main okunamadı")
    return info


def _safe_name(name: str) -> bool:
    parts = name.split("/")
    return not name.startswith("/") and ".." not in parts and "\\" not in name


def split_jar(jar: Path, work: Path, root: Path, prefixes, excludes, lib_re, plugin_name):
    classes_dir = work / "classes"
    classes_dir.mkdir(parents=True, exist_ok=True)
    libs_dir = root / "libs"
    res_dir = root / "src" / "main" / "resources"
    res_dir.mkdir(parents=True, exist_ok=True)
    prefix_tuple = tuple(prefixes)
    exclude_tuple = tuple(excludes)
    maven_skip = re.compile(rf"^META-INF/maven/[^/]+/{re.escape(plugin_name)}/")

    n_classes = n_res = n_skipped = 0
    lib_entries = []
    with zipfile.ZipFile(jar) as zin:
        for info in zin.infolist():
            name = info.filename
            if info.is_dir():
                continue
            if not _safe_name(name):
                LOG.warning("güvenli olmayan yol atlandı: %s", name)
                continue
            is_class = name.endswith(".class")
            in_project = name.startswith(prefix_tuple) and not (exclude_tuple and name.startswith(exclude_tuple))
            if is_class and in_project:
                zin.extract(info, classes_dir)
                n_classes += 1
            elif name in SKIP_FILES or SIGNATURE_RE.match(name) or maven_skip.match(name):
                n_skipped += 1
            elif in_project or ("/" not in name and not is_class and not lib_re.match(name)):
                target = res_dir / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(zin.read(info))
                n_res += 1
            elif LICENSE_RE.match(name):
                (root / name).write_bytes(zin.read(info))
            else:
                lib_entries.append(info)

        libs_jar = libs_dir / LIBS_JAR_NAME
        if lib_entries:
            libs_dir.mkdir(parents=True, exist_ok=True)
            with zipfile.ZipFile(libs_jar, "w", zipfile.ZIP_DEFLATED) as zout:
                for info in lib_entries:
                    zi = zipfile.ZipInfo(info.filename, info.date_time)
                    zi.compress_type = zipfile.ZIP_DEFLATED
                    zi.external_attr = info.external_attr
                    zout.writestr(zi, zin.read(info))
        elif libs_jar.exists():
            libs_jar.unlink()
    LOG.info("proje sınıfı: %d, resource: %d, lib girdisi: %d, atlanan: %d",
             n_classes, n_res, len(lib_entries), n_skipped)
    if n_classes == 0:
        raise SystemExit("HATA: --project-prefix altında sınıf yok: " + ", ".join(prefixes))
    return classes_dir, (libs_dir / LIBS_JAR_NAME if lib_entries else None), n_classes, n_res, len(lib_entries)


def run_vineflower(vf: Path, classes_dir: Path, libs_jar, out_dir: Path) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    cmd = ["java", "-Xmx4g", "-jar", str(vf), "-dgs=1", "-asc=1", "-rsy=1", "-log=WARN"]
    if libs_jar is not None:
        cmd.append(f"-e={libs_jar}")
    cmd += [str(classes_dir), str(out_dir)]
    LOG.info("Vineflower çalışıyor")
    LOG.debug("komut: %s", " ".join(cmd))
    proc = subprocess.run(cmd, check=False)
    if proc.returncode != 0:
        LOG.warning("Vineflower çıkış kodu %d (kısmi çıktı olabilir)", proc.returncode)


def main() -> int:
    ap = argparse.ArgumentParser(description="Bukkit/Spigot/Paper eklenti jar'ı -> Gradle projesi")
    ap.add_argument("--jar", required=True, type=Path)
    ap.add_argument("--root", default=".", type=Path)
    ap.add_argument("--project-prefix", action="append", required=True,
                    help="decompile edilecek paket yolu, örn. com/artillexstudios/axvaults/ (birden fazla verilebilir)")
    ap.add_argument("--exclude-prefix", action="append", default=[],
                    help="proje prefix'i içinde olup kütüphane sayılan yol, örn. com/artillexstudios/axvaults/libs/")
    ap.add_argument("--lib-resource", default=DEFAULT_LIB_RESOURCE_RE,
                    help="jar kökündeki, kütüphaneye ait resource dosyalarını yakalayan regex")
    ap.add_argument("--vineflower", default=VINEFLOWER_VERSION)
    ap.add_argument("--force", action="store_true", help="mevcut src/main/java üzerine yaz (elle düzenlemeleri siler)")
    ap.add_argument("--split-only", action="store_true", help="decompile etme, sadece resource ve libs ayır")
    ap.add_argument("--legacy-fixes", action="store_true",
                    help="mixin duck cast ve getValue() ? kalıbı düzeltmelerini de uygula (Fabric modları için)")
    ap.add_argument("-v", "--verbose", action="store_true")
    args = ap.parse_args()
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="[%(levelname)s] %(message)s",
    )

    jar = args.jar.resolve()
    root = args.root.resolve()
    if not jar.is_file():
        LOG.error("jar bulunamadı: %s", jar)
        return 2
    if not args.split_only and shutil.which("java") is None:
        LOG.error("java PATH'te yok")
        return 2

    java_dir = root / "src" / "main" / "java"
    if not args.split_only and java_dir.exists() and any(java_dir.rglob("*.java")) and not args.force:
        LOG.error("%s dolu. Elle düzenleme yaptıysan --force ile çalıştırmak hepsini siler.", java_dir)
        return 3

    try:
        lib_re = re.compile(args.lib_resource)
    except re.error as exc:
        LOG.error("--lib-resource geçersiz regex: %s", exc)
        return 2

    with zipfile.ZipFile(jar) as zin:
        bad = zin.testzip()
        if bad:
            LOG.error("jar bozuk, ilk bozuk girdi: %s", bad)
            return 2
        plugin = read_plugin_info(zin)
    LOG.info("eklenti: %s %s, main=%s, api-version=%s",
             plugin["name"], plugin.get("version", "?"), plugin["main"], plugin.get("api-version", "?"))

    prefixes = [p if p.endswith("/") else p + "/" for p in args.project_prefix]
    excludes = [p if p.endswith("/") else p + "/" for p in args.exclude_prefix]

    with tempfile.TemporaryDirectory() as tmp:
        work = Path(tmp)
        classes_dir, libs_jar, n_classes, n_res, n_libs = split_jar(
            jar, work, root, prefixes, excludes, lib_re, plugin["name"]
        )
        if args.split_only:
            LOG.info("--split-only: decompile atlandı")
            return 0

        vf = work / "vineflower.jar"
        download(
            f"https://github.com/Vineflower/vineflower/releases/download/"
            f"{args.vineflower}/vineflower-{args.vineflower}.jar",
            vf,
        )

        raw_out = work / "decompiled"
        run_vineflower(vf, classes_dir, libs_jar, raw_out)

        sources = sorted(raw_out.rglob("*.java"))
        if not sources:
            LOG.error("Vineflower hiç .java üretmedi; src/main/java'ya dokunulmadı")
            return 1

        if java_dir.exists():
            shutil.rmtree(java_dir)
        java_dir.mkdir(parents=True)

        failed = []
        total = 0
        for src in sources:
            rel = src.relative_to(raw_out)
            text = src.read_text(encoding="utf-8", errors="replace")
            if any(marker in text for marker in FAIL_MARKERS):
                failed.append(rel.as_posix())
            text = fix_indy_concat(text)
            text = fix_bool_int(text)
            text = fix_raw_foreach(text)
            if args.legacy_fixes:
                text = fix_duck_casts(text)
                text = fix_getvalue_cond(text)
            dest = java_dir / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(text, encoding="utf-8")
            total += 1

    main_rel = plugin["main"].replace(".", "/") + ".java"
    main_ok = (java_dir / main_rel).is_file()
    report = [
        "# Decompile raporu",
        "",
        f"- Kaynak jar: `{jar.name}`",
        f"- Eklenti: `{plugin['name']} {plugin.get('version', '?')}`, main: `{plugin['main']}`"
        + ("" if main_ok else " (main sınıfı çıkmadı!)"),
        f"- Vineflower: `{args.vineflower}`",
        f"- Decompile edilen sınıf: {n_classes}",
        f"- Yazılan .java dosyası: {total}",
        f"- Resource: {n_res}",
        f"- libs/{LIBS_JAR_NAME} girdisi: {n_libs}",
        f"- Sorunlu dosya: {len(failed)}",
        "",
        "Sorunlu dosyalar elle düzeltilmeli (decompiler tam çıkaramadı):",
        "",
    ]
    report += [f"- `{f}`" for f in failed] or ["- yok"]
    (root / "DECOMPILE_REPORT.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    LOG.info("bitti: %d dosya, %d sorunlu", total, len(failed))
    return 0 if total > 0 else 1


if __name__ == "__main__":
    sys.exit(main())
