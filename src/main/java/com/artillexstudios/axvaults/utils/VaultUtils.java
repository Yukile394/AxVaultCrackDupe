package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.serializers.Serializers;
import com.artillexstudios.axvaults.libs.axapi.utils.logging.LogUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import java.util.concurrent.CompletableFuture;

public class VaultUtils {
   private static boolean asyncItemSerializer;
   private static boolean deleteEmptyVaults;

   public static void reload() {
      asyncItemSerializer = AxVaults.CONFIG.getBoolean("async-item-serializer", false);
      deleteEmptyVaults = AxVaults.CONFIG.getBoolean("delete-empty-vaults", true);
   }

   public static CompletableFuture<Void> save(Vault vault) {
      CompletableFuture<Object> local = new CompletableFuture<>();
      ThreadUtils.runAsync(() -> serialize(vault, local));
      CompletableFuture<Void> cf = new CompletableFuture<>();
      local.exceptionally(throwable -> {
         LogUtils.error("An exception occurred while saving vaults!", throwable);
         return null;
      }).thenAccept(result -> ThreadUtils.runAsync(() -> {
            AxVaults.getDatabase().saveVault(vault, result);
            cf.complete(null);
         }));
      return cf;
   }

   public static void serialize(Vault vault, CompletableFuture<Object> future) {
      Runnable runnable = () -> {
         if (deleteEmptyVaults && vault.getStorage().isEmpty()) {
            future.complete(true);
         } else {
            try {
               future.complete(Serializers.ITEM_ARRAY.serialize(vault.getStorage().getContents()));
            } catch (Exception var3) {
               var3.printStackTrace();
               future.complete(null);
            }
         }
      };
      if (asyncItemSerializer) {
         runnable.run();
      } else {
         ThreadUtils.runSync(runnable);
      }
   }

   public static boolean isAsyncItemSerializer() {
      return asyncItemSerializer;
   }

   public static boolean isDeleteEmptyVaults() {
      return deleteEmptyVaults;
   }
}
