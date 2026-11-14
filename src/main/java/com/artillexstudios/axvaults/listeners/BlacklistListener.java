package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.utils.ContainerUtils;
import com.artillexstudios.axvaults.libs.axapi.utils.PaperUtils;
import com.artillexstudios.axvaults.utils.BlacklistUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlacklistListener implements Listener {
   @EventHandler
   public void onClick(@NotNull InventoryClickEvent event) {
      if (PaperUtils.getHolder(event.getInventory(), false) instanceof Vault) {
         ItemStack item = this.getItem(event);
         if (BlacklistUtils.isBlacklisted(item)) {
            if (event.getView().getTopInventory().equals(event.getClickedInventory())) {
               ItemStack copy = item.clone();
               item.setAmount(0);
               ContainerUtils.INSTANCE.addOrDrop((Player)event.getWhoClicked(), List.of(copy));
            }

            event.setCancelled(true);
            AxVaults.MESSAGEUTILS.sendLang(event.getWhoClicked(), "banned-item");
         }
      }
   }

   @Nullable
   private ItemStack getItem(InventoryClickEvent event) {
      if (event.getClickedInventory() != null) {
         if (event.getClick() == ClickType.SWAP_OFFHAND && event.getClickedInventory().getType() != InventoryType.PLAYER) {
            return event.getWhoClicked().getInventory().getItemInOffHand();
         }

         if (event.getClick() == ClickType.NUMBER_KEY) {
            Inventory inventory = event.getClickedInventory().getType() == InventoryType.PLAYER
               ? event.getView().getTopInventory()
               : event.getView().getBottomInventory();
            return inventory.getItem(event.getHotbarButton());
         }
      }

      return event.getCurrentItem();
   }
}
