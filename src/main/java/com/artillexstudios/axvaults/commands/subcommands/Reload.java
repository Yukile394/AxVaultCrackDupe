package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.listeners.PlayerInteractListener;
import com.artillexstudios.axvaults.utils.DebugUtils;
import com.artillexstudios.axvaults.utils.VaultUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public enum Reload {
   INSTANCE;

   public void execute(CommandSender sender) {
      Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00[AxVaults] &#AAFFAAReloading configuration..."));
      if (!AxVaults.CONFIG.reload()) {
         AxVaults.MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "config.yml"));
      } else {
         Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00\u2560 &#00FF00Reloaded &fconfig.yml&#00FF00!"));
         if (!AxVaults.MESSAGES.reload()) {
            AxVaults.MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "messages.yml"));
         } else {
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00\u2560 &#00FF00Reloaded &fmessages.yml&#00FF00!"));
            VaultUtils.reload();
            DebugUtils.reload();
            PlayerInteractListener.reload();
            VaultManager.getVaults().forEach(Vault::reload);
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00\u2560 &#00FF00Reloaded &fvaults&#00FF00!"));
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00\u255a &#00FF00Successful reload!"));
            AxVaults.MESSAGEUTILS.sendLang(sender, "reload.success");
         }
      }
   }
}
