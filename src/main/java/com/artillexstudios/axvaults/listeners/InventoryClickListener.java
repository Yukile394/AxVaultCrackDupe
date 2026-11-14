package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.libs.axapi.utils.PaperUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {
   @EventHandler
   public void onClick(InventoryClickEvent event) {
      Player player = (Player)event.getWhoClicked();
      if (PaperUtils.getHolder(event.getInventory(), false) instanceof Vault) {
         if (AxVaults.isStopping()) {
            event.setCancelled(true);
         } else {
            boolean openSelector = AxVaults.CONFIG.getBoolean("clicking-outside-open-selector", false);
            if (openSelector
               && player.hasPermission("axvaults.selector")
               && event.getAction() == InventoryAction.NOTHING
               && event.getClickedInventory() == null) {
               VaultManager.getPlayer(player).thenAccept(vaultPlayer -> new VaultSelector(player, vaultPlayer).open());
            }
         }
      }
   }
}
