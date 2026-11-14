package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.util.Collections;
import java.util.HashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum ForceOpen {
   INSTANCE;

   public void execute(CommandSender sender, Player player, @Nullable Integer number) {
      VaultManager.getPlayer(player).thenAccept(vaultPlayer -> {
         if (number != null) {
            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%num%", number + "");
            replacements.put("%player%", player.getName());
            Vault vault = vaultPlayer.getVault(number);
            if (vault == null) {
               AxVaults.MESSAGEUTILS.sendLang(player, "vault.not-unlocked", replacements);
            } else {
               vault.open(player);
               AxVaults.MESSAGEUTILS.sendLang(player, "vault.opened", replacements);
               AxVaults.MESSAGEUTILS.sendLang(sender, "force-open-vault", replacements);
            }
         } else {
            new VaultSelector(player, vaultPlayer).open();
            AxVaults.MESSAGEUTILS.sendLang(sender, "force-open", Collections.singletonMap("%player%", player.getName()));
         }
      });
   }
}
