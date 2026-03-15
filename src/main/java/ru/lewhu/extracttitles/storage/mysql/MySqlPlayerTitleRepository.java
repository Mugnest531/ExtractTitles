package ru.lewhu.extracttitles.storage.mysql;

import ru.lewhu.extracttitles.domain.player.OwnershipSource;
import ru.lewhu.extracttitles.domain.player.PlayerTitleProfile;
import ru.lewhu.extracttitles.domain.player.TitleOwnership;
import ru.lewhu.extracttitles.storage.repository.PlayerTitleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MySqlPlayerTitleRepository implements PlayerTitleRepository {
    private final MySqlStorage storage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "extracttitles-mysql");
        t.setDaemon(true);
        return t;
    });

    public MySqlPlayerTitleRepository(MySqlStorage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<PlayerTitleProfile> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerTitleProfile profile = new PlayerTitleProfile(uuid);
            try {
                loadProfile(profile);
                loadOwnership(profile);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return profile;
        }, executor);
    }

    private void loadProfile(PlayerTitleProfile profile) throws SQLException {
        String sql = "SELECT active_title FROM player_profiles WHERE uuid = ?";
        try (PreparedStatement st = storage.connection().prepareStatement(sql)) {
            st.setString(1, profile.uuid().toString());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    profile.setActiveTitleId(rs.getString("active_title"));
                }
            }
        }
    }

    private void loadOwnership(PlayerTitleProfile profile) throws SQLException {
        String sql = "SELECT * FROM title_ownership WHERE uuid = ?";
        try (PreparedStatement st = storage.connection().prepareStatement(sql)) {
            st.setString(1, profile.uuid().toString());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    profile.addOwnership(new TitleOwnership(
                            rs.getString("title_id"),
                            rs.getInt("permanent") == 1,
                            rs.getLong("start_at"),
                            rs.getLong("expires_at"),
                            OwnershipSource.fromString(rs.getString("source"))
                    ));
                }
            }
        }
    }

    @Override
    public CompletableFuture<Void> save(PlayerTitleProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try {
                Connection connection = storage.connection();
                connection.setAutoCommit(false);
                try {
                    saveProfile(profile, connection);
                    deleteOwnershipBatch(profile, connection);
                    insertOwnershipBatch(profile, connection);
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private void saveProfile(PlayerTitleProfile profile, Connection connection) throws SQLException {
        String upsert = """
                INSERT INTO player_profiles(uuid, active_title, updated_at)
                VALUES(?, ?, ?)
                ON DUPLICATE KEY UPDATE active_title = VALUES(active_title), updated_at = VALUES(updated_at)
                """;
        try (PreparedStatement st = connection.prepareStatement(upsert)) {
            st.setString(1, profile.uuid().toString());
            st.setString(2, profile.activeTitleId());
            st.setLong(3, Instant.now().toEpochMilli());
            st.executeUpdate();
        }
    }

    private void deleteOwnershipBatch(PlayerTitleProfile profile, Connection connection) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("DELETE FROM title_ownership WHERE uuid = ?")) {
            st.setString(1, profile.uuid().toString());
            st.executeUpdate();
        }
    }

    private void insertOwnershipBatch(PlayerTitleProfile profile, Connection connection) throws SQLException {
        String insert = "INSERT INTO title_ownership(uuid, title_id, permanent, start_at, expires_at, source) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = connection.prepareStatement(insert)) {
            for (TitleOwnership ownership : profile.ownerships()) {
                st.setString(1, profile.uuid().toString());
                st.setString(2, ownership.titleId());
                st.setInt(3, ownership.permanent() ? 1 : 0);
                st.setLong(4, ownership.startAt());
                st.setLong(5, ownership.expiresAt());
                st.setString(6, ownership.source().name());
                st.addBatch();
            }
            st.executeBatch();
        }
    }

    @Override
    public CompletableFuture<Void> deleteOwnership(UUID uuid, String titleId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement st = storage.connection().prepareStatement("DELETE FROM title_ownership WHERE uuid = ? AND title_id = ?")) {
                st.setString(1, uuid.toString());
                st.setString(2, titleId.toLowerCase());
                st.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
