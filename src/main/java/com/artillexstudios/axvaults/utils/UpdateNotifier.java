package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.config.Config;
import com.artillexstudios.axvaults.libs.axapi.scheduler.Scheduler;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

public class UpdateNotifier implements Listener {
   private static Config config;
   private static Config lang;
   private static boolean onJoin;
   private static String prefix;
   private static String updateNotifier;
   private final String current;
   private String latest = null;
   private boolean newest = true;

   public static void init(Config config, Config lang) {
      UpdateNotifier.config = config;
      UpdateNotifier.lang = lang;
      reload();
   }

   public static void reload() {
      onJoin = config.getBoolean("update-notifier.on-join", true);
      prefix = config.getString("prefix");
      updateNotifier = lang.getString("update-notifier");
   }

   public UpdateNotifier() {
      this.current = AxVaults.getInstance().getDescription().getVersion();
      AxVaults.getInstance().getServer().getPluginManager().registerEvents(this, AxVaults.getInstance());
      long time = 36000L;
      Scheduler.get().runAsyncTimer(t -> {
         this.latest = this.readVersion();
         this.newest = !this.isOutdated(this.current);
         if (this.latest != null && !this.newest) {
            Scheduler.get().runLaterAsync(t2 -> Bukkit.getConsoleSender().sendMessage(this.getMessage()), 50L);
            t.cancel();
         }
      }, 0L, time);
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      if (this.latest != null && !this.newest) {
         if (onJoin) {
            if (event.getPlayer().hasPermission(AxVaults.getInstance().getName().toLowerCase() + ".update-notify")) {
               Scheduler.get().runLaterAsync(t -> event.getPlayer().sendMessage(this.getMessage()), 50L);
            }
         }
      }
   }

   private String getMessage() {
      HashMap<String, String> map = new HashMap<>();
      map.put("%current%", this.current);
      map.put("%latest%", this.latest);
      return StringUtils.formatToString(String.format("%s %s", prefix, updateNotifier), map);
   }

   @Nullable
   private String readVersion() {
      try {
         String var4;
         try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
               .uri(new URI("https://www.artillex-studios.com/api/v1/resource/%s/latest-version".formatted(AxVaults.getInstance().getName())))
               .timeout(Duration.of(10L, ChronoUnit.SECONDS))
               .GET()
               .build();
            HttpResponse<?> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
               return null;
            }

            var4 = response.body().toString();
         }

         return var4;
      } catch (Exception var7) {
         return null;
      }
   }

   public String getLatest() {
      return this.latest;
   }

   public boolean isOutdated(String current) {
      if (this.latest == null) {
         return false;
      } else {
         String[] parts1 = this.latest.split("\\.");
         String[] parts2 = current.split("\\.");

         for (int i = 0; i < 3; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 > num2) {
               return true;
            }

            if (num1 < num2) {
               return false;
            }
         }

         return false;
      }
   }
}
