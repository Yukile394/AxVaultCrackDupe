package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.utils.PaperUtils;
import com.artillexstudios.axvaults.utils.SoundUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public class InventoryCloseListener implements Listener {
   @EventHandler
   public void onClose(@NotNull InventoryCloseEvent event) {
      if (PaperUtils.getHolder(event.getInventory(), false) instanceof Vault vault) {
         AxVaults.getThreadedQueue().submit(() -> {
            AxVaults.MESSAGEUTILS.sendLang(event.getPlayer(), "vault.closed", Map.of("%num%", vault.getId() + ""));
            SoundUtils.playSound((Player)event.getPlayer(), AxVaults.MESSAGES.getString("sounds.close"));
         });
      }
   }
}
