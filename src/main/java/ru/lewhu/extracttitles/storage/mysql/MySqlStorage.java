package ru.lewhu.extracttitles.storage.mysql;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class MySqlStorage {
    private final JavaPlugin plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public MySqlStorage(JavaPlugin plugin, String host, int port, String database, String username, String password) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void init() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC";
        connection = DriverManager.getConnection(url, username, password);

        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_profiles (
                  uuid VARCHAR(36) PRIMARY KEY,
                  active_title VARCHAR(128) NULL,
                  updated_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS title_ownership (
                  uuid VARCHAR(36) NOT NULL,
                  title_id VARCHAR(128) NOT NULL,
                  permanent TINYINT(1) NOT NULL,
                  start_at BIGINT NOT NULL,
                  expires_at BIGINT NOT NULL,
                  source VARCHAR(64) NOT NULL,
                  PRIMARY KEY(uuid, title_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }

        plugin.getLogger().info("MySQL storage initialized.");
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
