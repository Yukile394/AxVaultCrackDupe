package com.artillexstudios.axvaults.converters;

import com.artillexstudios.axvaults.libs.axapi.config.Config;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.utils.VaultUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class PlayerVaultsXConverter {
   public void run() {
      File path = new File(Bukkit.getWorldContainer(), "plugins/PlayerVaults/newvaults");
      if (!path.exists()) {
         Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF0000[AxVaults] Failed converting! Folder not found!"));
      } else {
         int vaults = 0;
         int players = 0;

         for (File file : path.listFiles()) {
            if (file.getName().endsWith(".yml")) {
               Config data = new Config(file);

               UUID uuid;
               try {
                  uuid = UUID.fromString(file.getName().replace(".yml", ""));
               } catch (Exception var15) {
                  continue;
               }

               players++;
               VaultPlayer vaultPlayer = VaultManager.getPlayer(Bukkit.getOfflinePlayer(uuid)).join();

               for (String route : data.getBackingDocument().getRoutesAsStrings(false)) {
                  int num = Integer.parseInt(route.replace("vault", ""));
                  Vault vault = new Vault(vaultPlayer, num, null, this.getItems(data.getString(route)));
                  VaultUtils.save(vault);
                  vaults++;
               }
            }
         }

         Bukkit.getConsoleSender()
            .sendMessage(StringUtils.formatToString("&#33FF33[AxVaults] Finished converting " + vaults + " vaults of " + players + " players!"));
      }
   }

   private ItemStack[] getItems(String base64) {
      try {
         Class<?> cl = Class.forName("com.drtshock.playervaults.vaultmanagement.CardboardBoxSerialization");
         Method method = cl.getDeclaredMethod("fromStorage", String.class, String.class);
         method.setAccessible(true);
         return (ItemStack[])method.invoke(null, base64, "AxVaults");
      } catch (Exception var4) {
         var4.printStackTrace();
         return null;
      }
   }
}
