package com.artillexstudios.axvaults.database.messaging;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.database.impl.MySQL;
import com.artillexstudios.axvaults.libs.axapi.executor.ExceptionReportingScheduledThreadPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SQLMessaging {
   private static ScheduledExecutorService executor = null;

   public static void start() {
      String support = AxVaults.CONFIG.getString("multi-server-support", null);
      if (support != null) {
         if (support.equalsIgnoreCase("none")) {
            AxVaults.CONFIG.remove("multi-server-support");
            AxVaults.CONFIG.save();
         } else if (AxVaults.getDatabase() instanceof MySQL db) {
            if (executor != null) {
               executor.shutdown();
            }

            executor = new ExceptionReportingScheduledThreadPool(1);
            executor.scheduleAtFixedRate(db::checkForChanges, 5L, 5L, TimeUnit.SECONDS);
            executor.scheduleAtFixedRate(db::removeOldChanges, 10L, 10L, TimeUnit.SECONDS);
         } else {
            AxVaults.CONFIG.remove("multi-server-support");
            AxVaults.CONFIG.save();
         }
      }
   }

   public static void stop() {
      if (executor != null) {
         executor.shutdown();
      }
   }
}
