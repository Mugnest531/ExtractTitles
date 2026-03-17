package ru.lewhu.extracttitles.domain.player;

public enum OwnershipSource {
    PURCHASE,
    ADMIN_GRANT,
    ADMIN_TEMP_GRANT,
    CONSOLE,
    MIGRATION,
    UNKNOWN;

    public static OwnershipSource fromString(String raw) {
        if (raw == null || raw.isBlank()) return UNKNOWN;
        try {
            return OwnershipSource.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
