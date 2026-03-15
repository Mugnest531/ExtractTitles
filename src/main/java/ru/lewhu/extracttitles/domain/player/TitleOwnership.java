package ru.lewhu.extracttitles.domain.player;

public final class TitleOwnership {
    private final String titleId;
    private final boolean permanent;
    private final long startAt;
    private final long expiresAt;
    private final OwnershipSource source;

    public TitleOwnership(String titleId, boolean permanent, long startAt, long expiresAt, OwnershipSource source) {
        this.titleId = titleId.toLowerCase();
        this.permanent = permanent;
        this.startAt = startAt;
        this.expiresAt = expiresAt;
        this.source = source == null ? OwnershipSource.UNKNOWN : source;
    }

    public String titleId() { return titleId; }
    public boolean permanent() { return permanent; }
    public long startAt() { return startAt; }
    public long expiresAt() { return expiresAt; }
    public OwnershipSource source() { return source; }

    public boolean isExpired(long now) {
        return !permanent && expiresAt > 0 && now >= expiresAt;
    }
}
