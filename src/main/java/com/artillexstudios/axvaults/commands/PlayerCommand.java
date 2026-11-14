package com.artillexstudios.axvaults.commands;

import com.artillexstudios.axvaults.commands.subcommands.Open;
import com.artillexstudios.axvaults.libs.lamp.annotation.AutoComplete;
import com.artillexstudios.axvaults.libs.lamp.annotation.DefaultFor;
import com.artillexstudios.axvaults.libs.lamp.annotation.Optional;
import com.artillexstudios.axvaults.libs.lamp.orphan.OrphanCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerCommand implements OrphanCommand {
   @DefaultFor({"~"})
   @AutoComplete("@vaults")
   public void vault(@NotNull Player sender, @Optional Integer number) {
      Open.INSTANCE.execute(sender, number, false);
   }
}
