package com.artillexstudios.axvaults.vaults;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.hooks.HookManager;
import com.artillexstudios.axvaults.libs.axapi.utils.ContainerUtils;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.utils.SoundUtils;
import com.artillexstudios.axvaults.utils.ThreadUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class Vault implements InventoryHolder {
   private final VaultPlayer vaultPlayer;
   private Inventory storage;
   private final int id;
   private Material icon;
   private long lastOpen = System.currentTimeMillis();
   private final AtomicBoolean changed = new AtomicBoolean(false);
   private final List<ItemStack> overflow = new ArrayList<>();

   public Vault(VaultPlayer vaultPlayer, int id, Material icon, @Nullable ItemStack[] contents) {
      this.vaultPlayer = vaultPlayer;
      this.id = id;
      this.storage = Bukkit.createInventory(this, vaultPlayer.getRows() * 9, this.getTitle());
      if (contents != null) {
         this.setContents(contents);
      }

      vaultPlayer.getVaultMap().put(id, this);
      this.icon = icon;
   }

   @Internal
   public void setContents(ItemStack[] items) {
      if (this.storage.getSize() >= items.length) {
         this.storage.setContents(items);
      } else {
         for (int i = 0; i < this.storage.getSize(); i++) {
            this.storage.setItem(i, items[i]);
         }

         for (int i = this.storage.getSize(); i < items.length; i++) {
            if (items[i] != null) {
               this.overflow.add(items[i]);
            }
         }
      }
   }

   public Inventory getStorage() {
      return this.storage;
   }

   public VaultPlayer getVaultPlayer() {
      return this.vaultPlayer;
   }

   public UUID getUUID() {
      return this.vaultPlayer.getUUID();
   }

   public int getId() {
      return this.id;
   }

   public long getLastOpen() {
      return this.lastOpen;
   }

   public AtomicBoolean hasChanged() {
      return this.changed;
   }

   public void setIcon(Material icon) {
      this.changed.set(true);
      this.icon = icon;
   }

   public Material getIcon() {
      return this.icon == null ? Material.valueOf(AxVaults.MESSAGES.getString("guis.selector.item-owned.material", "BARREL")) : this.icon;
   }

   @Nullable
   public Material getRealIcon() {
      return this.icon;
   }

   public int getSlotsFilled() {
      int am = 0;

      for (ItemStack it : this.storage.getContents()) {
         if (it != null) {
            am++;
         }
      }

      return am;
   }

   public void open(@NotNull Player player) {
      if (!AxVaults.isStopping()) {
         ThreadUtils.runSync(player, () -> {
            this.changed.set(true);
            if (this.vaultPlayer.getRows() * 9 != this.storage.getSize()) {
               this.reload();
            }

            if (!this.overflow.isEmpty()) {
               this.dropOverFlow(player);
            }

            player.openInventory(this.storage);
            SoundUtils.playSound(player, AxVaults.MESSAGES.getString("sounds.open"));
            this.lastOpen = System.currentTimeMillis();
         });
      }
   }

   private void dropOverFlow(@NotNull Player player) {
      this.changed.set(true);
      Iterator<ItemStack> it = this.overflow.iterator();

      while (it.hasNext()) {
         ItemStack itemStack = it.next();
         it.remove();
         ContainerUtils.INSTANCE.addOrDrop(this.storage, List.of(itemStack), player.getLocation());
      }
   }

   public boolean isOpened() {
      return !this.storage.getViewers().isEmpty();
   }

   public void reload() {
      Inventory newStorage = Bukkit.createInventory(this, this.vaultPlayer.getRows() * 9, this.getTitle());
      ItemStack[] contents = this.storage.getContents();
      List<HumanEntity> viewers = new ArrayList<>(this.storage.getViewers());
      Iterator<HumanEntity> viewerIterator = viewers.iterator();

      while (viewerIterator.hasNext()) {
         viewerIterator.next().openInventory(newStorage);
         viewerIterator.remove();
      }

      this.storage.clear();
      this.storage = newStorage;
      this.setContents(contents);
   }

   private String getTitle() {
      String title = AxVaults.MESSAGES.getString("guis.vault.title").replace("%num%", this.id + "");
      title = HookManager.getPlaceholderParser().setPlaceholders(Bukkit.getOfflinePlayer(this.vaultPlayer.getUUID()), title);
      return StringUtils.formatToString(title);
   }

   @Override
   public String toString() {
      return "Vault{vaultPlayer="
         + this.vaultPlayer.getUUID()
         + ", changed="
         + this.changed
         + ", lastOpen="
         + this.lastOpen
         + ", icon="
         + this.icon
         + ", id="
         + this.id
         + ", storage="
         + this.getSlotsFilled()
         + "/"
         + this.storage.getSize()
         + "}";
   }

   @NotNull
   public Inventory getInventory() {
      return this.storage;
   }
}
