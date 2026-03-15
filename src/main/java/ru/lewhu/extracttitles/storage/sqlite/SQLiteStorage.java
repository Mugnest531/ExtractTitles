package ru.lewhu.extracttitles.storage.sqlite;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLiteStorage {
    private final JavaPlugin plugin;
    private final String fileName;
    private Connection connection;

    public SQLiteStorage(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void init() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), fileName);
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку плагина для SQLite.");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_profiles (
                  uuid TEXT PRIMARY KEY,
                  active_title TEXT,
                  updated_at INTEGER NOT NULL
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS title_ownership (
                  uuid TEXT NOT NULL,
                  title_id TEXT NOT NULL,
                  permanent INTEGER NOT NULL,
                  start_at INTEGER NOT NULL,
                  expires_at INTEGER NOT NULL,
                  source TEXT NOT NULL,
                  PRIMARY KEY(uuid, title_id)
                )
            """);
        }
    }

    public Connection connection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
