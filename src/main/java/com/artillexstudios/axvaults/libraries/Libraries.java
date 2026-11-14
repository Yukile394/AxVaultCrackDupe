package com.artillexstudios.axvaults.libraries;

import com.artillexstudios.axvaults.libs.axapi.AxPlugin;
import com.artillexstudios.axvaults.libs.axapi.dependencies.DependencyManagerWrapper;
import com.artillexstudios.axvaults.libs.axapi.libraries.Library;
import com.artillexstudios.axvaults.libs.axapi.libraries.Relocation;
import com.artillexstudios.axvaults.libs.axapi.utils.file.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public enum Libraries {
   HIKARICP("com{}zaxxer:HikariCP:7.1.0", relocation("com{}zaxxer{}hikari", "com.artillexstudios.axvaults.libs.hikari")),
   SQLITE_JDBC("org{}xerial:sqlite-jdbc:3.53.2.0"),
   H2_JDBC("com{}h2database:h2:2.1.214"),
   MYSQL_CONNECTOR("com{}mysql:mysql-connector-j:9.2.0", relocation("com{}mysql", "com.artillexstudios.axvaults.libs.mysql"));

   private final List<Relocation> relocations = new ArrayList<>();
   private final Library library;

   public static void load(AxPlugin plugin, DependencyManagerWrapper manager) {
      File libs = new File(plugin.getDataFolder(), "libs");
      if (libs.exists()) {
         FileUtils.deleteNested(libs.toPath());
      }

      for (Libraries lib : values()) {
         for (Relocation relocation : lib.relocations()) {
            manager.relocate(relocation);
         }

         Libraries.FetchResult result = lib.fetchLibrary();
         if (result.exception()) {
            break;
         }

         manager.dependency(result.library());
      }
   }

   @NotNull
   public Libraries.FetchResult fetchLibrary() {
      return new Libraries.FetchResult(this.library, false);
   }

   private static Relocation relocation(String from, String to) {
      return new Relocation(from.replace("{}", "."), to);
   }

   public List<Relocation> relocations() {
      return this.relocations;
   }

   private Libraries(String lib, Relocation... relocation) {
      this(lib);
      this.relocations.addAll(Arrays.stream(relocation).toList());
   }

   private Libraries(String lib) {
      String[] split = lib.replace("{}", ".").split(":");
      this.library = new Library(split[0], split[1], split[2], null, List.of());
   }

   public static record FetchResult(Library library, boolean exception) {
   }
}
