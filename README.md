# AxVaults (decompile projesi)

Orijinal: **AxVaults** 2.16.1, yazar: **Artillex Studios** (Bukkit/Spigot/Paper eklentisi, Folia destekli).

## Lisans (önemli)

- Jar içinde lisans dosyası yok; `LICENSE` dosyası MIT metnidir, telif satırında yıl yoktur.
- Modrinth AxVaults sayfası **MIT** diyor, ama bunu 2.6.2 ve 2.10.0 sürümlerinde gördüm; **2.16.1 için doğrulamadım**.
- Repoyu public yapmadan önce yazarın GitHub reposundaki (Artillex-Studios) lisansa bak, gerekirse `LICENSE` dosyasını onunla değiştir.

## Hedef (jar'dan okundu)

| | |
|---|---|
| Eklenti | AxVaults 2.16.1, main `com.artillexstudios.axvaults.AxVaults` |
| api-version | 1.20 |
| Derleme API'si | spigot-api 1.20.2-R0.1-SNAPSHOT |
| Java | 21 |
| Opsiyonel | PlaceholderAPI (softdepend) |
| Gradle | 8.8 (wrapper) |
| Vineflower | 1.10.1 |

`slf4j-api 2.0.9` **tahmindir** (pom'da yok, `ThreadUtils` `org.slf4j.Logger` kullanıyor).

## Gömülü kütüphaneler

Jar'daki `com/artillexstudios/axvaults/libs/**` (axapi, lamp, triumph-gui, bstats), `net/kyori/**`, `org/jetbrains/**`, `org/intellij/**`, lamp `.properties` dosyaları ve `META-INF` kütüphane girdileri `libs/bundled-libs.jar` içine ayrılır. Build'de jar'a geri eklenir.

## Kullanım (telefondan)

1. Orijinal jar'ın adını telefonda `AxVaults.jar` yap, GitHub'da `original/` klasörüne yükle. GitHub web'de yeniden adlandırma jar'ı bozar.
2. Actions → **Bootstrap (decompile)** → Run workflow. İlk adım jar'ın boyutunu ve zip geçerliliğini kontrol eder.
3. Actions → **Build** → Run workflow. Jar: çalışmanın **Artifacts** bölümünde ve **Releases**'te.
4. Build kırmızıysa **Hata özeti** adımının çıktısını kopyalayıp gönder.

## Beklenmesi gerekenler

- Decompile çıktısı orijinal kaynakla birebir aynı olmaz. Bazı dosyalar elle düzeltilmeli; `DECOMPILE_REPORT.md` sorunlu dosyaları listeler.
- Bootstrap'i tekrar çalıştırmak `src/main/java`'yı siler. Dolu olduğunda **force** olmadan durur; force açıkken elle düzenleme commit'lerini listeleyip uyarır.
- `src/main/resources` ve `libs/` repoda hazır gelir; Java kaynakları ilk Bootstrap'te üretilir.
- PlaceholderAPI gerçek bağımlılık olarak alınır (public Maven deposu), stub gerekmez.

## Yapı

```
original/AxVaults.jar         orijinal jar
tools/decompile.py            ayrıştırma + Vineflower + düzeltmeler
libs/bundled-libs.jar         gömülü kütüphaneler
src/main/java                 decompile çıktısı
src/main/resources            plugin.yml, config.yml, messages.yml
```
