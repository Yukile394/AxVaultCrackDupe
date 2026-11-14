package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtils {
   private static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);

   public static void checkMain(String message) {
      if (!Scheduler.get().isGlobalTickThread()) {
         log.error("Thread {} failed main thread check for {}!", new Object[]{Thread.currentThread().getName(), message, new Throwable()});
         throw new RuntimeException();
      }
   }

   public static void checkNotMain(String message) {
      if (Scheduler.get().isGlobalTickThread()) {
         log.error("Thread {} failed main thread check for {}!", new Object[]{Thread.currentThread().getName(), message, new Throwable()});
         throw new RuntimeException();
      }
   }

   public static void runAsync(Runnable runnable) {
      if (AxVaults.isStopping()) {
         runSync(runnable);
      } else if (!Scheduler.get().isGlobalTickThread()) {
         runnable.run();
      } else {
         AxVaults.getThreadedQueue().submit(runnable);
      }
   }

   public static void runSync(Runnable runnable) {
      if (!Scheduler.get().isGlobalTickThread() && !AxVaults.isStopping()) {
         Scheduler.get().run(runnable);
      } else {
         runnable.run();
      }
   }

   public static void runSync(Player player, Runnable runnable) {
      if (!Scheduler.get().isOwnedByCurrentRegion(player.getLocation()) && !AxVaults.isStopping()) {
         Scheduler.get().run(player, task -> runnable.run(), runnable);
      } else {
         runnable.run();
      }
   }
}
