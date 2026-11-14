package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.converters.PlayerVaultsXConverter;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;

public enum Converter {
   INSTANCE;

   public void execute(CommandSender sender) {
      CompletableFuture.runAsync(() -> new PlayerVaultsXConverter().run());
      AxVaults.MESSAGEUTILS.sendLang(sender, "converter.started");
   }
}
