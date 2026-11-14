package com.artillexstudios.axvaults.guis;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axvaults.libs.axapi.reflection.ClassUtils;
import com.artillexstudios.axvaults.libs.axapi.utils.Cooldown;
import com.artillexstudios.axvaults.libs.axapi.utils.ItemBuilder;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.libs.gui.guis.Gui;
import com.artillexstudios.axvaults.libs.gui.guis.GuiItem;
import com.artillexstudios.axvaults.libs.gui.guis.PaginatedGui;
import com.artillexstudios.axvaults.utils.ThreadUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class VaultSelector {
   private static final Cooldown<Player> cooldown = Cooldown.createSynchronized();
   private final Player player;
   private final VaultPlayer vaultPlayer;

   public VaultSelector(Player player, VaultPlayer vaultPlayer) {
      this.player = player;
      this.vaultPlayer = vaultPlayer;
   }

   public void open() {
      this.open(1);
   }

   public void open(int page) {
      int rows = AxVaults.CONFIG.getInt("vault-selector-rows", 6);
      int pageSize = rows * 9 - 9;
      String title = AxVaults.MESSAGES.getString("guis.selector.title");
      if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
         title = PlaceholderAPI.setPlaceholders(this.player, title);
      }

      PaginatedGui gui = Gui.paginated().title(StringUtils.format(title)).rows(rows).pageSize(pageSize).disableAllInteractions().create();

      for (int i = 0; i < pageSize * (page + 1); i++) {
         this.getItemOfVault(this.player, i + 1, gui).thenAccept(guiItem -> {
            if (guiItem != null) {
               gui.addItem(guiItem);
               gui.update();
            }
         });
      }

      Section prev;
      if ((prev = AxVaults.MESSAGES.getSection("gui-items.previous-page")) != null) {
         GuiItem item1 = new GuiItem(ItemBuilder.create(prev).get());
         item1.setAction(event -> {
            if (!getOrAddCooldown((Player)event.getWhoClicked())) {
               gui.previous();
            }
         });
         gui.setItem(rows, 3, item1);
      }

      Section next;
      if ((next = AxVaults.MESSAGES.getSection("gui-items.next-page")) != null) {
         GuiItem item2 = new GuiItem(ItemBuilder.create(next).get());
         item2.setAction(event -> {
            if (!getOrAddCooldown((Player)event.getWhoClicked())) {
               gui.next();

               for (int i = 0; i < pageSize; i++) {
                  this.getItemOfVault(this.player, gui.getCurrentPageNum() * pageSize + i + 1, gui).thenAccept(guiItem -> {
                     if (guiItem != null) {
                        gui.addItem(guiItem);
                        gui.update();
                     }
                  });
               }
            }
         });
         gui.setItem(rows, 7, item2);
      }

      Section close;
      if ((close = AxVaults.MESSAGES.getSection("gui-items.close")) != null) {
         GuiItem item3 = new GuiItem(ItemBuilder.create(close).get());
         item3.setAction(event -> {
            if (!getOrAddCooldown((Player)event.getWhoClicked())) {
               event.getWhoClicked().closeInventory();
            }
         });
         gui.setItem(rows, 5, item3);
      }

      ThreadUtils.runSync(this.player, () -> gui.open(this.player, page));
   }

   private CompletableFuture<GuiItem> getItemOfVault(@NotNull Player player, int num, @NotNull PaginatedGui gui) {
      int maxVaults = AxVaults.CONFIG.getInt("max-vault-amount");
      if (maxVaults != -1 && num > maxVaults) {
         return CompletableFuture.completedFuture(null);
      } else {
         HashMap<String, String> replacements = new HashMap<>();
         replacements.put("%num%", num + "");
         Vault vault = this.vaultPlayer.getVault(num);
         CompletableFuture<GuiItem> cf = new CompletableFuture<>();
         AxVaults.getThreadedQueue().submit(() -> {
            if (vault != null) {
               replacements.put("%used%", vault.getSlotsFilled() + "");
               replacements.put("%max%", vault.getStorage().getSize() + "");
               ItemBuilder builder = ItemBuilder.create(AxVaults.MESSAGES.getSection("guis.selector.item-owned"));
               builder.setLore(AxVaults.MESSAGES.getStringList("guis.selector.item-owned.lore"), replacements);
               builder.setName(AxVaults.MESSAGES.getString("guis.selector.item-owned.name"), replacements);
               ItemStack it = builder.get();
               if (it.hasItemMeta()) {
                  ItemMeta meta = it.getItemMeta();
                  meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
                  it.setItemMeta(meta);
               }

               it.setType(vault.getIcon());
               switch (AxVaults.CONFIG.getInt("selector-item-amount-mode", 1)) {
                  case 1:
                     it.setAmount(num % 64 == 0 ? 64 : num % 64);
                     break;
                  case 3:
                     it.setAmount(Math.max(1, vault.getSlotsFilled()));
               }

               GuiItem guiItem = new GuiItem(it);
               guiItem.setAction(event -> {
                  if (!getOrAddCooldown((Player)event.getWhoClicked())) {
                     if (event.isShiftClick()) {
                        if (!player.hasPermission("axvaults.itempicker")) {
                           AxVaults.MESSAGEUTILS.sendLang(event.getWhoClicked(), "no-permission");
                        } else {
                           new ItemPicker(player, this.vaultPlayer).open(vault, gui.getCurrentPageNum(), 1);
                        }
                     } else {
                        AxVaults.MESSAGEUTILS.sendLang(event.getWhoClicked(), "vault.opened", replacements);
                        vault.open(player);
                     }
                  }
               });
               cf.complete(guiItem);
            } else {
               if (!AxVaults.CONFIG.getBoolean("show-locked-vaults", true)) {
                  cf.complete(null);
                  return;
               }

               ItemBuilder builder = ItemBuilder.create(AxVaults.MESSAGES.getSection("guis.selector.item-locked"));
               builder.setLore(AxVaults.MESSAGES.getStringList("guis.selector.item-locked.lore"), replacements);
               builder.setName(AxVaults.MESSAGES.getString("guis.selector.item-locked.name"), replacements);
               ItemStack it = builder.get();
               if (AxVaults.CONFIG.getInt("selector-item-amount-mode", 1) == 1) {
                  it.setAmount(num % 64 == 0 ? 64 : num % 64);
               }

               cf.complete(new GuiItem(it));
            }
         });
         return cf;
      }
   }

   private static boolean getOrAddCooldown(Player player) {
      long cooldownMillis = AxVaults.CONFIG.getLong("gui-refresh-cooldown-milliseconds", 100L);
      if (cooldownMillis > 0L) {
         if (cooldown.hasCooldown(player)) {
            return true;
         }

         cooldown.addCooldown(player, cooldownMillis);
      }

      return false;
   }
}
