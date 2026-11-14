package com.artillexstudios.axvaults.commands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.lamp.bukkit.BukkitCommandHandler;
import com.artillexstudios.axvaults.libs.lamp.orphan.Orphans;
import com.artillexstudios.axvaults.utils.CommandMessages;
import com.artillexstudios.axvaults.vaults.VaultManager;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandManager {
   private static BukkitCommandHandler handler = null;

   public static void load() {
      handler = BukkitCommandHandler.create(AxVaults.getInstance());
      handler.getAutoCompleter().registerSuggestion("vaults", (args, sender, command) -> {
         Player player = Bukkit.getPlayer(sender.getUniqueId());
         if (!player.hasPermission("axvaults.openremote")) {
            return new ArrayList<>();
         } else {
            ArrayList<String> numbers = new ArrayList<>();
            VaultPlayer vaultPlayer = VaultManager.getPlayerOrNull(player);
            if (vaultPlayer == null) {
               return numbers;
            } else {
               for (Integer i : vaultPlayer.getVaultMap().keySet()) {
                  numbers.add(String.valueOf(i));
               }

               return numbers;
            }
         }
      });
      handler.getTranslator().add(new CommandMessages());
      handler.setLocale(Locale.of("en", "US"));
      reload();
   }

   public static void reload() {
      handler.unregisterAllCommands();
      List<String> aliases = AxVaults.CONFIG.getStringList("player-command-aliases");
      if (!aliases.isEmpty()) {
         handler.register(new Object[]{Orphans.path(aliases.toArray(String[]::new)).handler(new PlayerCommand())});
      }

      aliases = AxVaults.CONFIG.getStringList("admin-command-aliases");
      if (!aliases.isEmpty()) {
         handler.register(new Object[]{Orphans.path(aliases.toArray(String[]::new)).handler(new AdminCommand())});
      }

      handler.registerBrigadier();
   }
}
