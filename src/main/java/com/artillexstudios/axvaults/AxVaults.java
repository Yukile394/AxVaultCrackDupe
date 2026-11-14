package com.artillexstudios.axvaults;

import com.artillexstudios.axvaults.commands.CommandManager;
import com.artillexstudios.axvaults.database.Database;
import com.artillexstudios.axvaults.database.impl.H2;
import com.artillexstudios.axvaults.database.impl.MySQL;
import com.artillexstudios.axvaults.database.impl.SQLite;
import com.artillexstudios.axvaults.database.messaging.SQLMessaging;
import com.artillexstudios.axvaults.hooks.HookManager;
import com.artillexstudios.axvaults.libraries.Libraries;
import com.artillexstudios.axvaults.libs.axapi.AxPlugin;
import com.artillexstudios.axvaults.libs.axapi.config.Config;
import com.artillexstudios.axvaults.libs.axapi.dependencies.DependencyManagerWrapper;
import com.artillexstudios.axvaults.libs.axapi.executor.ThreadedQueue;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.dvs.versioning.BasicVersioning;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axvaults.libs.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axvaults.libs.axapi.metrics.AxMetrics;
import com.artillexstudios.axvaults.libs.axapi.utils.MessageUtils;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.libs.axapi.utils.featureflags.FeatureFlags;
import com.artillexstudios.axvaults.libs.axapi.utils.logging.LoggerNameFormat;
import com.artillexstudios.axvaults.libs.bstats.bukkit.Metrics;
import com.artillexstudios.axvaults.listeners.BlacklistListener;
import com.artillexstudios.axvaults.listeners.BlockBreakListener;
import com.artillexstudios.axvaults.listeners.InventoryClickListener;
import com.artillexstudios.axvaults.listeners.InventoryCloseListener;
import com.artillexstudios.axvaults.listeners.PlayerInteractListener;
import com.artillexstudios.axvaults.listeners.PlayerListeners;
import com.artillexstudios.axvaults.schedulers.AutoSaveScheduler;
import com.artillexstudios.axvaults.utils.DebugUtils;
import com.artillexstudios.axvaults.utils.UpdateNotifier;
import com.artillexstudios.axvaults.utils.VaultUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;

public final class AxVaults extends AxPlugin {
   private static boolean stopping = false;
   public static Config CONFIG;
   public static Config MESSAGES;
   public static MessageUtils MESSAGEUTILS;
   private static AxPlugin instance;
   private static ThreadedQueue<Runnable> threadedQueue;
   private static Database database;
   private static AxMetrics metrics;

   public static ThreadedQueue<Runnable> getThreadedQueue() {
      return threadedQueue;
   }

   public static AxPlugin getInstance() {
      return instance;
   }

   public static Database getDatabase() {
      return database;
   }

   public static boolean isStopping() {
      return stopping;
   }

   @Override
   public void dependencies(DependencyManagerWrapper manager) {
      instance = this;
      Libraries.load(instance, manager);
   }

   @Override
   public void enable() {
      new Metrics(this, 20541);
      CONFIG = new Config(
         new File(this.getDataFolder(), "config.yml"),
         this.getResource("config.yml"),
         GeneralSettings.builder().setUseDefaults(false).build(),
         LoaderSettings.builder().setAutoUpdate(true).build(),
         DumperSettings.DEFAULT,
         UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build()
      );
      MESSAGES = new Config(
         new File(this.getDataFolder(), "messages.yml"),
         this.getResource("messages.yml"),
         GeneralSettings.builder().setUseDefaults(false).build(),
         LoaderSettings.builder().setAutoUpdate(true).build(),
         DumperSettings.DEFAULT,
         UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build()
      );
      MESSAGEUTILS = new MessageUtils(MESSAGES.getBackingDocument(), "prefix", CONFIG.getBackingDocument());
      threadedQueue = new ThreadedQueue<>("AxVaults-Datastore-thread");
      VaultUtils.reload();
      HookManager.setupHooks();
      DebugUtils.init(CONFIG);
      String var1 = CONFIG.getString("database.type").toLowerCase();

      database = (Database)(switch (var1) {
         case "sqlite" -> new SQLite();
         case "mysql" -> new MySQL();
         default -> new H2();
      });
      database.setup();
      threadedQueue.submit(() -> database.load());
      this.getServer().getPluginManager().registerEvents(new PlayerListeners(), this);
      this.getServer().getPluginManager().registerEvents(new BlacklistListener(), this);
      this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
      this.getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
      this.getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
      this.getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
      CommandManager.load();
      AutoSaveScheduler.start();
      SQLMessaging.start();
      metrics = new AxMetrics(this, 3L);
      metrics.start();
      Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55ff00[AxVaults] Loaded plugin!"));
      UpdateNotifier.init(CONFIG, MESSAGES);
      if (CONFIG.getBoolean("update-notifier.enabled", true)) {
         new UpdateNotifier();
      }
   }

   @Override
   public void disable() {
      stopping = true;
      if (metrics != null) {
         metrics.cancel();
      }

      for (Vault vault : VaultManager.getVaults()) {
         for (HumanEntity humanEntity : new ArrayList(vault.getInventory().getViewers())) {
            humanEntity.closeInventory();
         }
      }

      AutoSaveScheduler.stop();
      List<CompletableFuture<?>> futures = new ArrayList<>();

      for (Vault vault : VaultManager.getVaults()) {
         futures.add(VaultUtils.save(vault));
      }

      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
      SQLMessaging.stop();
      database.disable();
      threadedQueue.stop();
   }

   @Override
   public void updateFlags() {
      FeatureFlags.LOGGER_NAME_FORMAT.set(LoggerNameFormat.NAMEABLE);
   }
}
