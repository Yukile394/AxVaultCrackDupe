package com.artillexstudios.axvaults.database.impl;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.database.Database;
import com.artillexstudios.axvaults.libs.axapi.serializers.Serializers;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.libs.hikari.HikariConfig;
import com.artillexstudios.axvaults.libs.hikari.HikariDataSource;
import com.artillexstudios.axvaults.placed.PlacedVaults;
import com.artillexstudios.axvaults.utils.ThreadUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MySQL implements Database {
   private HikariDataSource dataSource;
   private final ArrayList<Integer> acknowledged = new ArrayList<>();
   private final HashMap<Integer, Long> sentFromHere = new HashMap<>();

   public MySQL() {
      Bukkit.getConsoleSender()
         .sendMessage(
            StringUtils.formatToString(
               "&#FF0000[AxVaults] MySQL is NOT fully supported! It will continue to work and you can ignore this warning, however there might be issues."
            )
         );
   }

   @Override
   public String getType() {
      return "MySQL";
   }

   @Override
   public void setup() {
      HikariConfig hConfig = new HikariConfig();
      hConfig.setPoolName("axvaults-pool");
      hConfig.setMaximumPoolSize(AxVaults.CONFIG.getInt("database.pool.maximum-pool-size"));
      hConfig.setMinimumIdle(AxVaults.CONFIG.getInt("database.pool.minimum-idle"));
      hConfig.setMaxLifetime((long)AxVaults.CONFIG.getInt("database.pool.maximum-lifetime"));
      hConfig.setKeepaliveTime((long)AxVaults.CONFIG.getInt("database.pool.keepalive-time"));
      hConfig.setConnectionTimeout((long)AxVaults.CONFIG.getInt("database.pool.connection-timeout"));
      hConfig.setDriverClassName("com.artillexstudios.axvaults.libs.mysql.cj.jdbc.Driver");
      hConfig.setJdbcUrl(
         "jdbc:mysql://"
            + AxVaults.CONFIG.getString("database.address")
            + ":"
            + AxVaults.CONFIG.getString("database.port")
            + "/"
            + AxVaults.CONFIG.getString("database.database")
      );
      hConfig.addDataSourceProperty("user", AxVaults.CONFIG.getString("database.username"));
      hConfig.addDataSourceProperty("password", AxVaults.CONFIG.getString("database.password"));
      this.dataSource = new HikariDataSource(hConfig);
      String CREATE_TABLE = "    CREATE TABLE IF NOT EXISTS `axvaults_data`(\n      `id` INT(128) NOT NULL,\n      `uuid` VARCHAR(36) NOT NULL,\n      `storage` LONGBLOB,\n      `icon` VARCHAR(128)\n    );\n";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(CREATE_TABLE);
      ) {
         stmt.executeUpdate();
      } catch (SQLException var23) {
         var23.printStackTrace();
      }

      String CREATE_TABLE2 = "    CREATE TABLE IF NOT EXISTS `axvaults_blocks` (\n      `location` VARCHAR(255) NOT NULL,\n      `number` INT,\n      PRIMARY KEY (`location`)\n    );\n";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(CREATE_TABLE2);
      ) {
         stmt.executeUpdate();
      } catch (SQLException var20) {
         var20.printStackTrace();
      }

      String CREATE_TABLE3 = "    CREATE TABLE IF NOT EXISTS axvaults_messages (\n      id INT NOT NULL AUTO_INCREMENT,\n      event TINYINT,\n      vault_id INT NOT NULL,\n      uuid VARCHAR(36) NOT NULL,\n      date BIGINT NOT NULL,\n      PRIMARY KEY (id)\n    );\n";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(CREATE_TABLE3);
      ) {
         stmt.executeUpdate();
      } catch (SQLException var17) {
         var17.printStackTrace();
      }
   }

   @Override
   public void saveVault(Vault vault, Object result) {
      if (result instanceof Boolean bool && bool) {
         String sql = "DELETE FROM axvaults_data WHERE uuid = ? AND id = ?;";

         try (
            Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
         ) {
            stmt.setString(1, vault.getUUID().toString());
            stmt.setInt(2, vault.getId());
            stmt.executeUpdate();
         } catch (SQLException var20) {
            var20.printStackTrace();
         }

         return;
      }

      if (result == null) {
         Bukkit.getConsoleSender()
            .sendMessage(StringUtils.formatToString("&#FF0000[AxVaults] Failed to save vault #%s of %s!".formatted(vault.getId(), vault.getUUID().toString())));
      } else {
         byte[] bytes = (byte[])result;
         String sql = "SELECT * FROM axvaults_data WHERE uuid = ? AND id = ?;";

         try (
            Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
         ) {
            stmt.setString(1, vault.getUUID().toString());
            stmt.setInt(2, vault.getId());

            try (ResultSet rs = stmt.executeQuery()) {
               if (rs.next()) {
                  sql = "UPDATE axvaults_data SET storage = ?, icon = ? WHERE uuid = ? AND id = ?;";

                  try (PreparedStatement stmt2 = conn.prepareStatement(sql)) {
                     stmt2.setBytes(1, bytes);
                     stmt2.setString(2, vault.getRealIcon() == null ? null : vault.getRealIcon().name());
                     stmt2.setString(3, vault.getUUID().toString());
                     stmt2.setInt(4, vault.getId());
                     stmt2.executeUpdate();
                  }
               } else {
                  sql = "INSERT INTO axvaults_data(id, uuid, storage, icon) VALUES (?, ?, ?, ?);";

                  try (PreparedStatement stmt2 = conn.prepareStatement(sql)) {
                     stmt2.setInt(1, vault.getId());
                     stmt2.setString(2, vault.getUUID().toString());
                     stmt2.setBytes(3, bytes);
                     stmt2.setString(4, vault.getRealIcon() == null ? null : vault.getRealIcon().name());
                     stmt2.executeUpdate();
                  }
               }
            }
         } catch (SQLException var26) {
            var26.printStackTrace();
         }
      }
   }

   @Override
   public void loadVaults(@NotNull VaultPlayer vaultPlayer) {
      String sql = "SELECT * FROM axvaults_data WHERE uuid = ?;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM axvaults_data WHERE uuid = ?;");
      ) {
         stmt.setString(1, vaultPlayer.getUUID().toString());

         try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
               int id = rs.getInt(1);

               ItemStack[] items;
               try {
                  items = Serializers.ITEM_ARRAY.deserialize(rs.getBytes(3));
               } catch (Exception var12) {
                  var12.printStackTrace();
                  Bukkit.getConsoleSender()
                     .sendMessage(
                        StringUtils.formatToString("&#FF0000[AxVaults] Failed to load vault #%s of %s!".formatted(id, vaultPlayer.getUUID().toString()))
                     );
                  continue;
               }

               Material icon = rs.getString(4) == null ? null : Material.valueOf(rs.getString(4));
               new Vault(vaultPlayer, id, icon, items);
            }
         }
      } catch (SQLException var16) {
         var16.printStackTrace();
      }
   }

   @Override
   public boolean isVault(@NotNull Location location) {
      String sql = "SELECT * FROM axvaults_blocks WHERE location = ?;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM axvaults_blocks WHERE location = ?;");
      ) {
         stmt.setString(1, Serializers.LOCATION.serialize(location));

         try (ResultSet rs = stmt.executeQuery()) {
            return rs.next();
         }
      } catch (SQLException var14) {
         var14.printStackTrace();
         return false;
      }
   }

   @Override
   public void setVault(@NotNull Location location, @Nullable Integer num) {
      String sql = "INSERT INTO `axvaults_blocks`(`location`, `number`) VALUES (?, ?)";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("INSERT INTO `axvaults_blocks`(`location`, `number`) VALUES (?, ?)");
      ) {
         stmt.setString(1, Serializers.LOCATION.serialize(location));
         if (num == null) {
            stmt.setString(2, null);
         } else {
            stmt.setInt(2, num);
         }

         stmt.executeUpdate();
      } catch (SQLException var12) {
         var12.printStackTrace();
      }

      PlacedVaults.addVault(location, num);
   }

   @Override
   public void removeVault(@NotNull Location location) {
      String sql = "DELETE FROM axvaults_blocks WHERE location = ?;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("DELETE FROM axvaults_blocks WHERE location = ?;");
      ) {
         stmt.setString(1, Serializers.LOCATION.serialize(location));
         stmt.executeUpdate();
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void deleteVault(@NotNull UUID uuid, int num) {
      String sql = "DELETE FROM axvaults_data WHERE uuid = ? AND id = ?;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("DELETE FROM axvaults_data WHERE uuid = ? AND id = ?;");
      ) {
         stmt.setString(1, uuid.toString());
         stmt.setInt(2, num);
         stmt.executeUpdate();
      } catch (SQLException var12) {
         var12.printStackTrace();
      }
   }

   @Override
   public void load() {
      String sql = "SELECT * FROM axvaults_blocks;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM axvaults_blocks;");
         ResultSet rs = stmt.executeQuery();
      ) {
         while (rs.next()) {
            String vault = rs.getString(2);
            Integer vaultInt = vault == null ? null : Integer.parseInt(vault);
            PlacedVaults.addVault(Serializers.LOCATION.deserialize(rs.getString(1)), vaultInt);
         }
      } catch (SQLException var13) {
         var13.printStackTrace();
      }
   }

   private void sendMessage(@NotNull MySQL.ChangeType changeType, int id, UUID uuid) {
      if (!AxVaults.CONFIG.getString("multi-server-support", "none").equalsIgnoreCase("none")) {
         String sql = "INSERT INTO axvaults_messages(event, vault_id, uuid, date) VALUES (?, ?, ?, ?);";

         try (
            Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO axvaults_messages(event, vault_id, uuid, date) VALUES (?, ?, ?, ?);", 1);
         ) {
            stmt.setShort(1, (short)changeType.ordinal());
            stmt.setInt(2, id);
            stmt.setString(3, uuid.toString());
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
               if (rs.next()) {
                  this.sentFromHere.put(rs.getInt(1), System.currentTimeMillis() + 10000L);
               }
            }
         } catch (SQLException var16) {
            var16.printStackTrace();
         }
      }
   }

   public void checkForChanges() {
      if (!AxVaults.CONFIG.getString("multi-server-support", "none").equalsIgnoreCase("none")) {
         String sql = "SELECT * FROM axvaults_messages;";

         try (
            Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM axvaults_messages;");
            ResultSet rs = stmt.executeQuery();
         ) {
            while (rs.next()) {
               if (this.sentFromHere.containsKey(rs.getInt(1))) {
                  return;
               }

               if (!this.acknowledged.contains(rs.getInt(1))) {
                  this.acknowledged.add(rs.getInt(1));
                  int num = rs.getInt(3);
                  UUID uuid = UUID.fromString(rs.getString(4));
                  switch (MySQL.ChangeType.entries[rs.getInt(2)]) {
                     case UPDATE:
                        VaultPlayer vp = VaultManager.getPlayers().get(uuid);
                        if (vp == null) {
                           return;
                        }

                        Vault vault = vp.getVault(num);
                        if (vault == null) {
                           return;
                        }

                        this.updateVault(vault);
                  }
               }
            }
         } catch (SQLException var15) {
            var15.printStackTrace();
         }
      }
   }

   public void removeOldChanges() {
      String sql = "DELETE FROM axvaults_messages WHERE ? - 7500 > date;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("DELETE FROM axvaults_messages WHERE ? - 7500 > date;");
      ) {
         stmt.setLong(1, System.currentTimeMillis());
         stmt.executeUpdate();
         this.acknowledged.clear();
         this.sentFromHere.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
      } catch (SQLException var10) {
         var10.printStackTrace();
      }
   }

   private void updateVault(@NotNull Vault vault) {
      String sql = "SELECT * FROM axvaults_data WHERE uuid = ? AND id = ?;";

      try (
         Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM axvaults_data WHERE uuid = ? AND id = ?;");
      ) {
         stmt.setString(1, vault.getUUID().toString());
         stmt.setInt(2, vault.getId());

         try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
               ItemStack[] items = Serializers.ITEM_ARRAY.deserialize(rs.getBytes(3));
               ThreadUtils.runSync(() -> vault.setContents(items));
               vault.setIcon(rs.getString(4) == null ? null : Material.valueOf(rs.getString(4)));
            }
         }
      } catch (SQLException var14) {
         var14.printStackTrace();
      }
   }

   @Override
   public void disable() {
      try {
         this.dataSource.close();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   private static enum ChangeType {
      DELETE,
      UPDATE,
      INSERT;

      public static final MySQL.ChangeType[] entries = values();
   }
}
