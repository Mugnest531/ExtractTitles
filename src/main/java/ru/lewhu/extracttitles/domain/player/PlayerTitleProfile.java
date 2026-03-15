package ru.lewhu.extracttitles.domain.player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerTitleProfile {
    private final UUID uuid;
    private final Map<String, TitleOwnership> ownerships = new HashMap<>();
    private String activeTitleId;

    public PlayerTitleProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public Collection<TitleOwnership> ownerships() {
        return Collections.unmodifiableCollection(ownerships.values());
    }

    public Map<String, TitleOwnership> ownershipMap() {
        return ownerships;
    }

    public boolean hasTitle(String titleId, long now) {
        TitleOwnership ownership = ownerships.get(titleId.toLowerCase());
        return ownership != null && !ownership.isExpired(now);
    }

    public TitleOwnership ownership(String titleId) {
        return ownerships.get(titleId.toLowerCase());
    }

    public void addOwnership(TitleOwnership ownership) {
        ownerships.put(ownership.titleId(), ownership);
    }

    public void removeOwnership(String titleId) {
        ownerships.remove(titleId.toLowerCase());
    }

    public String activeTitleId() {
        return activeTitleId;
    }

    public void setActiveTitleId(String activeTitleId) {
        this.activeTitleId = activeTitleId == null ? null : activeTitleId.toLowerCase();
    }
}
