package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.items.WrappedItemStack;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.block.implementation.Section;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BlacklistUtils {
   public static boolean isBlacklisted(@Nullable ItemStack it) {
      if (it == null || it.getType() == Material.AIR) {
         return false;
      } else if (checkLegacy(it)) {
         return true;
      } else {
         try {
            List<Map<String, Object>> list = AxVaults.CONFIG.getMapList("blacklist-items");
            if (list == null || list.isEmpty()) {
               return false;
            }

            WrappedItemStack wrap = WrappedItemStack.wrap(it);

            for (Map<String, Object> map : list) {
               ItemMatcher matcher = new ItemMatcher(wrap, map);
               boolean result = matcher.isMatching();
               if (result) {
                  return true;
               }
            }
         } catch (Exception var7) {
            var7.printStackTrace();
         }

         return false;
      }
   }

   private static boolean checkLegacy(ItemStack it) {
      Section section = AxVaults.CONFIG.getSection("blacklisted-items");
      if (section == null) {
         return false;
      } else {
         for (String s : section.getRoutesAsStrings(false)) {
            if (AxVaults.CONFIG.getString("blacklisted-items." + s + ".material") != null) {
               if (it.getType().toString().equalsIgnoreCase(AxVaults.CONFIG.getString("blacklisted-items." + s + ".material"))) {
                  return true;
               }
            } else if (AxVaults.CONFIG.getString("blacklisted-items." + s + ".name-contains") != null
               && it.getItemMeta() != null
               && it.getItemMeta().getDisplayName().contains(AxVaults.CONFIG.getString("blacklisted-items." + s + ".name-contains"))) {
               return true;
            }
         }

         return false;
      }
   }
}
