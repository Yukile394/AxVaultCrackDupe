package com.artillexstudios.axvaults.vaults;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.utils.PermissionUtils;
import com.artillexstudios.axvaults.utils.ThreadUtils;
import com.artillexstudios.axvaults.utils.VaultUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultPlayer {
   private final UUID uuid;
   private final ConcurrentHashMap<Integer, Vault> vaultMap = new ConcurrentHashMap<>();
   private boolean loaded = false;

   public VaultPlayer(UUID uuid) {
      this.uuid = uuid;
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public void load() {
      AxVaults.getDatabase().loadVaults(this);
      this.loaded = true;
   }

   public void load(CompletableFuture<VaultPlayer> cf) {
      this.load();
      ThreadUtils.runSync(() -> cf.complete(this));
   }

   public ConcurrentHashMap<Integer, Vault> getVaultMap() {
      return this.vaultMap;
   }

   @Nullable
   public Vault getVault(int num) {
      Player player = Bukkit.getPlayer(this.uuid);
      if (player != null) {
         if (!PermissionUtils.hasPermission(player, num)) {
            return null;
         }

         if (!this.vaultMap.containsKey(num)) {
            return this.addVault(new Vault(this, num, null, null));
         }
      }

      return !this.vaultMap.containsKey(num) ? null : this.vaultMap.get(num);
   }

   @NotNull
   public Vault addVault(@NotNull Vault vault) {
      if (this.vaultMap.containsKey(vault.getId())) {
         return this.vaultMap.get(vault.getId());
      } else {
         this.vaultMap.put(vault.getId(), vault);
         return vault;
      }
   }

   @Nullable
   public Vault removeVault(@NotNull Vault vault) {
      return this.vaultMap.remove(vault.getId());
   }

   public int getRows() {
      Player player = Bukkit.getPlayer(this.uuid);
      int def = AxVaults.CONFIG.getInt("vault-storage-rows", 6);
      if (player == null) {
         return def;
      } else {
         for (int i = 6; i >= 1; i--) {
            if (player.hasPermission("axvaults.rows." + i)) {
               return i;
            }
         }

         return def;
      }
   }

   public void save() {
      AxVaults.getThreadedQueue().submit(() -> {
         for (Vault vaultx : this.vaultMap.values()) {
            VaultUtils.save(vaultx);
         }
      });

      for (Vault vault : this.vaultMap.values()) {
         this.close(vault.getStorage());
      }
   }

   private void close(@NotNull Inventory inventory) {
      List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
      Iterator<HumanEntity> viewerIterator = viewers.iterator();

      while (viewerIterator.hasNext()) {
         viewerIterator.next().closeInventory();
         viewerIterator.remove();
      }
   }

   @Override
   public String toString() {
      return "VaultPlayer{uuid=" + this.uuid + ", vaultMap=" + this.vaultMap.size() + ", loaded=" + this.loaded + "}";
   }
}
