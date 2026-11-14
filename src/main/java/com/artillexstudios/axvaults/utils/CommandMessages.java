package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.libs.lamp.locales.LocaleReader;
import java.util.Locale;

public class CommandMessages implements LocaleReader {
   private final Locale locale = new Locale("en", "US");

   @Override
   public boolean containsKey(String s) {
      return true;
   }

   @Override
   public String get(String s) {
      return StringUtils.formatToString(
         AxVaults.CONFIG.getString("prefix", "")
            + switch (s) {
               case "invalid-enum", "invalid-number", "invalid-uuid", "invalid-url", "invalid-boolean" -> AxVaults.MESSAGES
               .getString("commands.invalid-value")
               .replace("%value%", "{0}");
               case "missing-argument" -> AxVaults.MESSAGES.getString("commands.missing-argument").replace("%value%", "{0}");
               case "no-permission" -> AxVaults.MESSAGES.getString("commands.no-permission");
               case "number-not-in-range" -> AxVaults.MESSAGES
               .getString("commands.out-of-range")
               .replace("%number%", "{0}")
               .replace("%min%", "{1}")
               .replace("%max%", "{2}");
               case "must-be-player" -> AxVaults.MESSAGES.getString("commands.player-only");
               case "must-be-console" -> AxVaults.MESSAGES.getString("commands.console-only");
               case "invalid-player" -> AxVaults.MESSAGES.getString("commands.invalid-player").replace("%player%", "{0}");
               case "invalid-selector" -> AxVaults.MESSAGES.getString("commands.invalid-selector");
               default -> AxVaults.MESSAGES.getString("commands.invalid-command");
            }
      );
   }

   @Override
   public Locale getLocale() {
      return this.locale;
   }
}
