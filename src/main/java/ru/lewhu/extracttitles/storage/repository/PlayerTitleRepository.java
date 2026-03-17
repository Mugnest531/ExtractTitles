package ru.lewhu.extracttitles.storage.repository;

import ru.lewhu.extracttitles.domain.player.PlayerTitleProfile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerTitleRepository {
    CompletableFuture<PlayerTitleProfile> load(UUID uuid);

    CompletableFuture<Void> save(PlayerTitleProfile profile);

    CompletableFuture<Void> deleteOwnership(UUID uuid, String titleId);

    void shutdown();
}
