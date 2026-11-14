package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import org.bukkit.command.CommandSender;

public enum Help {
   INSTANCE;

   public void execute(CommandSender sender) {
      for (String m : AxVaults.MESSAGES.getStringList("help")) {
         sender.sendMessage(StringUtils.formatToString(m));
      }
   }
}
