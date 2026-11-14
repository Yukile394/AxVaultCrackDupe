package com.artillexstudios.axvaults.schedulers;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.executor.ExceptionReportingScheduledThreadPool;
import com.artillexstudios.axvaults.libs.axapi.utils.mutable.MutableInteger;
import com.artillexstudios.axvaults.utils.VaultUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AutoSaveScheduler {
   private static int autoSaveMinutes = 1;
   private static ExceptionReportingScheduledThreadPool pool = null;
   private static long lastSave = -1L;
   private static long savedVaults = -1L;
   private static final Runnable saveRunnable = () -> {
      long saveStart = System.currentTimeMillis();
      MutableInteger saved = new MutableInteger();
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      try {
         for (Vault vault : VaultManager.getVaults()) {
            if (vault.hasChanged().get()) {
               futures.add(VaultUtils.save(vault));
               saved.increment();
            }

            if (!vault.isOpened()) {
               vault.hasChanged().set(false);
               if (Bukkit.getPlayer(vault.getUUID()) == null && System.currentTimeMillis() - vault.getLastOpen() > (long)(autoSaveMinutes - 1) * 1000L) {
                  VaultManager.removeVault(vault);
               }
            }
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
         lastSave = System.currentTimeMillis() - saveStart;
         savedVaults = (long)saved.get().intValue();
      });
   };

   public static void start() {
      autoSaveMinutes = AxVaults.CONFIG.getInt("auto-save-minutes");
      if (pool != null) {
         pool.shutdown();
      }

      pool = new ExceptionReportingScheduledThreadPool(1);
      pool.scheduleAtFixedRate(saveRunnable, (long)autoSaveMinutes, (long)autoSaveMinutes, TimeUnit.MINUTES);
   }

   public static void stop() {
      if (pool != null) {
         pool.shutdown();
      }
   }

   public static long getLastSaveLength() {
      return lastSave;
   }

   public static long getSavedVaults() {
      return savedVaults;
   }

   @Internal
   public static Runnable getSaveRunnable() {
      return saveRunnable;
   }
}
