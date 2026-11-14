package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.util.HashMap;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum Open {
   INSTANCE;

   public void execute(Player sender, @Nullable Integer number, boolean force) {
      if (number == null) {
         if (!force && !sender.hasPermission("axvaults.selector")) {
            AxVaults.MESSAGEUTILS.sendLang(sender, "no-permission");
         } else {
            VaultManager.getPlayer(sender).thenAccept(vaultPlayer -> new VaultSelector(sender, vaultPlayer).open());
         }
      } else if (number >= 1) {
         HashMap<String, String> replacements = new HashMap<>();
         replacements.put("%num%", number + "");
         if (!force && !sender.hasPermission("axvaults.openremote")) {
            AxVaults.MESSAGEUTILS.sendLang(sender, "no-permission");
         } else {
            VaultManager.getPlayer(sender).thenAccept(vaultPlayer -> {
               Vault vault = vaultPlayer.getVault(number);
               if (vault == null) {
                  AxVaults.MESSAGEUTILS.sendLang(sender, "vault.not-unlocked", replacements);
               } else {
                  vault.open(sender);
                  AxVaults.MESSAGEUTILS.sendLang(sender, "vault.opened", replacements);
               }
            });
         }
      }
   }
}
