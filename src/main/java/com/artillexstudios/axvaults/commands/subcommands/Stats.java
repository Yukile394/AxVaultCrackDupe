package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.schedulers.AutoSaveScheduler;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.util.HashMap;
import org.bukkit.command.CommandSender;

public enum Stats {
   INSTANCE;

   public void execute(CommandSender sender) {
      HashMap<String, String> replacements = new HashMap<>();
      replacements.put("%players%", String.valueOf(VaultManager.getPlayers().size()));
      replacements.put("%vaults%", String.valueOf(VaultManager.getVaults().size()));
      int vaults2 = VaultManager.getPlayers().values().stream().mapToInt(value -> value.getVaultMap().size()).sum();
      replacements.put("%vaults2%", vaults2 + "");
      long lastSave = AutoSaveScheduler.getLastSaveLength();
      long savedVaults = AutoSaveScheduler.getSavedVaults();
      replacements.put("%auto-save%", lastSave == -1L ? "---" : lastSave + "");
      replacements.put("%saved-vaults%", savedVaults == -1L ? "---" : savedVaults + "");

      for (String s : AxVaults.MESSAGES.getStringList("stats")) {
         AxVaults.MESSAGEUTILS.sendFormatted(sender, s, replacements);
      }
   }
}
