package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SoundUtils {
   private static final Pattern SPLIT = Pattern.compile("\\|");

   public static void playSound(@NotNull Player player, @Nullable String sound) {
      if (sound != null) {
         if (!sound.isBlank()) {
            try {
               float volume = 1.0F;
               float pitch = 1.0F;
               String[] split = SPLIT.split(sound);
               if (split.length == 3) {
                  volume = Float.parseFloat(split[1]);
                  pitch = Float.parseFloat(split[2]);
               }

               player.playSound(player, split[0], volume, pitch);
            } catch (Exception var5) {
               Bukkit.getConsoleSender()
                  .sendMessage(
                     StringUtils.formatToString("&#FF0000[AxVaults] The sound %sound% does not exist, this is a configuration issue!".replace("%sound%", sound))
                  );
            }
         }
      }
   }
}
