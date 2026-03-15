package ru.lewhu.extracttitles.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.domain.player.OwnershipSource;
import ru.lewhu.extracttitles.domain.player.PlayerTitleProfile;
import ru.lewhu.extracttitles.domain.player.TitleOwnership;
import ru.lewhu.extracttitles.storage.repository.PlayerTitleRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTitleService {
    private final JavaPlugin plugin;
    private final PlayerTitleRepository repository;
    private final Map<UUID, PlayerTitleProfile> cache = new ConcurrentHashMap<>();

    public PlayerTitleService(JavaPlugin plugin, PlayerTitleRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<PlayerTitleProfile> load(UUID uuid) {
        PlayerTitleProfile cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return repository.load(uuid).thenApply(profile -> {
            cache.put(uuid, profile);
            return profile;
        });
    }

    public PlayerTitleProfile cachedOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerTitleProfile::new);
    }

    public CompletableFuture<Void> save(UUID uuid) {
        PlayerTitleProfile profile = cache.get(uuid);
        if (profile == null) return CompletableFuture.completedFuture(null);
        return repository.save(profile);
    }

    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uuid : cache.keySet()) {
            futures.add(save(uuid));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public boolean hasTitle(UUID uuid, String titleId) {
        return cachedOrCreate(uuid).hasTitle(titleId, Instant.now().toEpochMilli());
    }

    public void grant(UUID uuid, String titleId, boolean permanent, long expiresAt, OwnershipSource source) {
        long now = Instant.now().toEpochMilli();
        cachedOrCreate(uuid).addOwnership(new TitleOwnership(titleId, permanent, now, expiresAt, source));
    }

    public void revoke(UUID uuid, String titleId) {
        PlayerTitleProfile profile = cachedOrCreate(uuid);
        profile.removeOwnership(titleId);
        if (titleId.equalsIgnoreCase(profile.activeTitleId())) {
            profile.setActiveTitleId(null);
        }
    }

    public void setActive(UUID uuid, String titleId) {
        cachedOrCreate(uuid).setActiveTitleId(titleId);
    }

    public void clearActive(UUID uuid) {
        cachedOrCreate(uuid).setActiveTitleId(null);
    }

    public String getActive(UUID uuid) {
        return cachedOrCreate(uuid).activeTitleId();
    }

    public TitleOwnership ownership(UUID uuid, String titleId) {
        if (titleId == null || titleId.isBlank()) return null;
        return cachedOrCreate(uuid).ownership(titleId);
    }

    public int ownedCount(UUID uuid) {
        PlayerTitleProfile profile = cachedOrCreate(uuid);
        long now = Instant.now().toEpochMilli();
        int count = 0;
        for (var own : profile.ownerships()) {
            if (!own.isExpired(now)) count++;
        }
        return count;
    }

    public List<String> validateExpiry(UUID uuid) {
        PlayerTitleProfile profile = cachedOrCreate(uuid);
        long now = Instant.now().toEpochMilli();
        List<String> expired = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        for (var own : profile.ownerships()) {
            if (own.isExpired(now)) {
                expired.add(own.titleId());
                toRemove.add(own.titleId());
            }
        }
        for (String titleId : toRemove) {
            profile.removeOwnership(titleId);
            if (titleId.equalsIgnoreCase(profile.activeTitleId())) {
                profile.setActiveTitleId(null);
            }
        }
        return expired;
    }

    public long remaining(UUID uuid, String titleId) {
        var ownership = cachedOrCreate(uuid).ownership(titleId);
        if (ownership == null) return -1;
        if (ownership.permanent()) return Long.MAX_VALUE;
        return Math.max(0, ownership.expiresAt() - Instant.now().toEpochMilli());
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    public void runSync(Runnable action) {
        Bukkit.getScheduler().runTask(plugin, action);
    }
}
