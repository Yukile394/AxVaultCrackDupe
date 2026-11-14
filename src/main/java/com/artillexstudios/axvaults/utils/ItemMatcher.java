package com.artillexstudios.axvaults.utils;

import com.artillexstudios.axvaults.libs.axapi.items.WrappedItemStack;
import com.artillexstudios.axvaults.libs.axapi.items.components.DataComponents;
import com.artillexstudios.axvaults.libs.axapi.items.components.data.CustomModelData;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

public class ItemMatcher {
   private final WrappedItemStack wrapped;
   private final Map<String, Object> map;
   private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
   private int needMatch = 0;

   public ItemMatcher(WrappedItemStack wrapped, Map<String, Object> map) {
      this.wrapped = wrapped;
      this.map = map;
   }

   public boolean isMatching() {
      int matches = 0;
      if (this.material()) {
         matches++;
      }

      if (this.name()) {
         matches++;
      }

      if (this.customModelData()) {
         matches++;
      }

      return matches >= this.needMatch;
   }

   public boolean material() {
      Object val = this.map.getOrDefault("material", this.map.get("type"));
      if (val == null) {
         return false;
      } else {
         this.needMatch++;
         Material material = this.wrapped.get(DataComponents.MATERIAL);
         return material == null ? false : SimpleRegex.matches((String)val, material.toString());
      }
   }

   public boolean name() {
      Object val = this.map.get("name");
      if (val == null) {
         return false;
      } else {
         this.needMatch++;
         Component customName = this.wrapped.get(DataComponents.CUSTOM_NAME);
         if (customName == null) {
            return false;
         } else {
            String plain = this.plainSerializer.serialize(customName);
            return SimpleRegex.matches((String)val, plain);
         }
      }
   }

   public boolean customModelData() {
      if (this.map.get("custom-model-data") instanceof Integer num) {
         this.needMatch++;
         CustomModelData cmd = this.wrapped.get(DataComponents.CUSTOM_MODEL_DATA);
         return cmd != null && !cmd.floats().isEmpty() ? num == cmd.floats().getFirst().intValue() : false;
      } else {
         return false;
      }
   }
}
