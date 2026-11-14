package com.artillexstudios.axvaults.database.impl;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.database.Database;
import com.artillexstudios.axvaults.libs.axapi.serializers.Serializers;
import com.artillexstudios.axvaults.libs.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.placed.PlacedVaults;
import com.artillexstudios.axvaults.utils.ThreadUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SQLite implements Database {
   private Connection conn;

   @Override
   public String getType() {
      return "SQLite";
   }

   @Override
   public void setup() {
      try {
         Class.forName("org.sqlite.JDBC");
         this.conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s/data.db", AxVaults.getInstance().getDataFolder()));
      } catch (Exception var8) {
         throw new RuntimeException(var8);
      }

      String CREATE_TABLE = "    CREATE TABLE IF NOT EXISTS `axvaults_data`(\n      `id` INT(128) NOT NULL,\n      `uuid` VARCHAR(36) NOT NULL,\n      `storage` LONGBLOB,\n      `icon` VARCHAR(128)\n    );\n";

      try (PreparedStatement stmt = this.conn.prepareStatement(CREATE_TABLE)) {
         stmt.executeUpdate();
      } catch (SQLException var12) {
         var12.printStackTrace();
      }

      String CREATE_TABLE2 = "    CREATE TABLE IF NOT EXISTS `axvaults_blocks` (\n      `location` VARCHAR(255) NOT NULL,\n      `number` INT,\n      PRIMARY KEY (`location`)\n    );\n";

      try (PreparedStatement stmt = this.conn.prepareStatement(CREATE_TABLE2)) {
         stmt.executeUpdate();
      } catch (SQLException var10) {
         var10.printStackTrace();
      }
   }

   @Override
   public void saveVault(Vault vault, Object result) {
      if (result instanceof Boolean bool && bool) {
         String sql = "DELETE FROM axvaults_data WHERE uuid = ? AND id = ?;";

         try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
            stmt.setString(1, vault.getUUID().toString());
            stmt.setInt(2, vault.getId());
            stmt.executeUpdate();
         } catch (SQLException var16) {
            var16.printStackTrace();
         }

         return;
      }

      if (result == null) {
         Bukkit.getConsoleSender()
            .sendMessage(StringUtils.formatToString("&#FF0000[AxVaults] Failed to save vault #%s of %s!".formatted(vault.getId(), vault.getUUID().toString())));
      } else {
         byte[] bytes = (byte[])result;
         String sql = "SELECT * FROM axvaults_data WHERE uuid = ? AND id = ?;";

         try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
            stmt.setString(1, vault.getUUID().toString());
            stmt.setInt(2, vault.getId());

            try (ResultSet rs = stmt.executeQuery()) {
               if (rs.next()) {
                  sql = "UPDATE axvaults_data SET storage = ?, icon = ? WHERE uuid = ? AND id = ?;";

                  try (PreparedStatement stmt2 = this.conn.prepareStatement(sql)) {
                     stmt2.setBytes(1, bytes);
                     stmt2.setString(2, vault.getRealIcon() == null ? null : vault.getRealIcon().name());
                     stmt2.setString(3, vault.getUUID().toString());
                     stmt2.setInt(4, vault.getId());
                     stmt2.executeUpdate();
                  }
               } else {
                  sql = "INSERT INTO axvaults_data(id, uuid, storage, icon) VALUES (?, ?, ?, ?);";

                  try (PreparedStatement stmt2 = this.conn.prepareStatement(sql)) {
                     stmt2.setInt(1, vault.getId());
                     stmt2.setString(2, vault.getUUID().toString());
                     stmt2.setBytes(3, bytes);
                     stmt2.setString(4, vault.getRealIcon() == null ? null : vault.getRealIcon().name());
                     stmt2.executeUpdate();
                  }
               }
            }
         } catch (SQLException var21) {
            var21.printStackTrace();
         }
      }
   }

   @Override
   public void loadVaults(@NotNull VaultPlayer vaultPlayer) {
      String sql = "SELECT * FROM axvaults_data WHERE uuid = ?;";

      try (PreparedStatement stmt = this.conn.prepareStatement("SELECT * FROM axvaults_data WHERE uuid = ?;")) {
         stmt.setString(1, vaultPlayer.getUUID().toString());

         try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
               int id = rs.getInt(1);

               ItemStack[] items;
               try {
                  items = Serializers.ITEM_ARRAY.deserialize(rs.getBytes(3));
               } catch (Exception var10) {
                  var10.printStackTrace();
                  Bukkit.getConsoleSender()
                     .sendMessage(
                        StringUtils.formatToString("&#FF0000[AxVaults] Failed to load vault #%s of %s!".formatted(id, vaultPlayer.getUUID().toString()))
                     );
                  continue;
               }

               Material icon = rs.getString(4) == null ? null : Material.valueOf(rs.getString(4));
               ThreadUtils.runSync(() -> new Vault(vaultPlayer, id, icon, items));
            }
         }
      } catch (SQLException var13) {
         var13.printStackTrace();
      }
   }

   @Override
   public boolean isVault(@NotNull Location location) {
      String sql = "SELECT * FROM axvaults_blocks WHERE location = ?;";

      try {
         boolean var5;
         try (PreparedStatement stmt = this.conn.prepareStatement("SELECT * FROM axvaults_blocks WHERE location = ?;")) {
            stmt.setString(1, Serializers.LOCATION.serialize(location));

            try (ResultSet rs = stmt.executeQuery()) {
               if (!rs.next()) {
                  return false;
               }

               var5 = true;
            }
         }

         return var5;
      } catch (SQLException var11) {
         var11.printStackTrace();
         return false;
      }
   }

   @Override
   public void setVault(@NotNull Location location, @Nullable Integer num) {
      String sql = "INSERT INTO `axvaults_blocks`(`location`, `number`) VALUES (?, ?)";

      try (PreparedStatement stmt = this.conn.prepareStatement("INSERT INTO `axvaults_blocks`(`location`, `number`) VALUES (?, ?)")) {
         stmt.setString(1, Serializers.LOCATION.serialize(location));
         if (num == null) {
            stmt.setString(2, null);
         } else {
            stmt.setInt(2, num);
         }

         stmt.executeUpdate();
      } catch (SQLException var9) {
         var9.printStackTrace();
      }

      PlacedVaults.addVault(location, num);
   }

   @Override
   public void removeVault(@NotNull Location location) {
      String sql = "DELETE FROM axvaults_blocks WHERE location = ?;";

      try (PreparedStatement stmt = this.conn.prepareStatement("DELETE FROM axvaults_blocks WHERE location = ?;")) {
         stmt.setString(1, Serializers.LOCATION.serialize(location));
         stmt.executeUpdate();
      } catch (SQLException var8) {
         var8.printStackTrace();
      }
   }

   @Override
   public void deleteVault(@NotNull UUID uuid, int num) {
      String sql = "DELETE FROM axvaults_data WHERE uuid = ? AND id = ?;";

      try (PreparedStatement stmt = this.conn.prepareStatement("DELETE FROM axvaults_data WHERE uuid = ? AND id = ?;")) {
         stmt.setString(1, uuid.toString());
         stmt.setInt(2, num);
         stmt.executeUpdate();
      } catch (SQLException var9) {
         var9.printStackTrace();
      }
   }

   @Override
   public void load() {
      String sql = "SELECT * FROM axvaults_blocks;";

      try (
         PreparedStatement stmt = this.conn.prepareStatement("SELECT * FROM axvaults_blocks;");
         ResultSet rs = stmt.executeQuery();
      ) {
         while (rs.next()) {
            String vault = rs.getString(2);
            Integer vaultInt = vault == null ? null : Integer.parseInt(vault);
            PlacedVaults.addVault(Serializers.LOCATION.deserialize(rs.getString(1)), vaultInt);
         }
      } catch (SQLException var10) {
         var10.printStackTrace();
      }
   }

   @Override
   public void disable() {
      try {
         this.conn.close();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }
}
