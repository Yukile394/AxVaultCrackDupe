package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.AxVaults;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum Set {
   INSTANCE;

   public void execute(Player sender, @Nullable Integer number) {
      Block block = sender.getTargetBlockExact(5);
      if (block == null) {
         AxVaults.MESSAGEUTILS.sendLang(sender, "set.no-block");
      } else {
         AxVaults.getThreadedQueue().submit(() -> {
            if (AxVaults.getDatabase().isVault(block.getLocation())) {
               AxVaults.MESSAGEUTILS.sendLang(sender, "set.already");
            } else {
               AxVaults.getDatabase().setVault(block.getLocation(), number);
               AxVaults.MESSAGEUTILS.sendLang(sender, "set.success");
            }
         });
      }
   }
}
