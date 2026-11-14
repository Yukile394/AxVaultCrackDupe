package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.AxVaults;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

public class PermissionUtils {
   public static boolean hasPermission(@NotNull Player player, int vault) {
      if (AxVaults.CONFIG.getInt("permission-mode", 0) == 0) {
         return player.hasPermission("axvaults.vault." + vault);
      } else if (player.isOp()) {
         return true;
      } else {
         int max = player.hasPermission("axvaults.vault.1") ? 1 : 0;

         for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
            if (effectivePermission.getValue()) {
               if (effectivePermission.getPermission().equals("*")) {
                  return true;
               }

               if (effectivePermission.getPermission().startsWith("axvaults.vault.")) {
                  int value = Integer.parseInt(effectivePermission.getPermission().substring(effectivePermission.getPermission().lastIndexOf(46) + 1));
                  if (value > max) {
                     max = value;
                  }
               }
            }
         }

         return vault <= max;
      }
   }
}
