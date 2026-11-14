package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum View {
   INSTANCE;

   public void execute(Player sender, OfflinePlayer player, @Nullable Integer number) {
      Map<String, String> replacements = new HashMap<>();
      replacements.put("%player%", player.getName());
      if (number == null) {
         VaultManager.getPlayer(player)
            .thenAccept(
               vaultPlayer -> {
                  replacements.put(
                     "%vaults%",
                     vaultPlayer.getVaultMap()
                        .values()
                        .stream()
                        .filter(vault -> vault.getSlotsFilled() != 0)
                        .map(vault -> vault.getId() + "")
                        .collect(Collectors.joining(", "))
                  );
                  AxVaults.MESSAGEUTILS.sendLang(sender, "view.info", replacements);
               }
            );
      } else {
         replacements.put("%num%", number + "");
         VaultManager.getPlayer(player).thenAccept(vaultPlayer -> {
            Vault vault = vaultPlayer.getVault(number);
            if (vault == null) {
               AxVaults.MESSAGEUTILS.sendLang(sender, "view.not-found", replacements);
            } else {
               vault.open(sender);
               AxVaults.MESSAGEUTILS.sendLang(sender, "view.open", replacements);
            }
         });
      }
   }
}
