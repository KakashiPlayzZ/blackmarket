package com.kakashiplayz.blackmarket;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final BlackMarket plugin;
    private Connection connection;

    public DatabaseManager(BlackMarket plugin) {
        this.plugin = plugin;
    }

    public synchronized void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            File dataFolder = new File(plugin.getDataFolder(), "database.db");
            if (!dataFolder.exists()) {
                dataFolder.getParentFile().mkdirs();
            }

            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to SQLite database: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        // purchase_history
        executeAsync("CREATE TABLE IF NOT EXISTS purchase_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36)," +
                "player_name VARCHAR(16)," +
                "item VARCHAR(255)," +
                "price DOUBLE," +
                "date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");");

        // player_limits
        executeAsync("CREATE TABLE IF NOT EXISTS player_limits (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "purchases INTEGER DEFAULT 0" +
                ");");
    }

    public void executeAsync(String sql) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (connection == null || connection.isClosed()) return;
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.execute();
                }
            } catch (SQLException e) {
                // Ignore if it's a closed DB exception during shutdown
                if (!e.getMessage().contains("closed")) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void logPurchase(UUID uuid, String name, String itemName, double price) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (connection == null || connection.isClosed()) return;
                String sql = "INSERT INTO purchase_history (player_uuid, player_name, item, price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, name);
                    stmt.setString(3, itemName);
                    stmt.setDouble(4, price);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                // Ignore if closed DB
                if (!e.getMessage().contains("closed")) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void resetLimits() {
        executeAsync("DELETE FROM player_limits;");
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to verify SQLite connection: " + e.getMessage());
        }
        return connection;
    }
}
