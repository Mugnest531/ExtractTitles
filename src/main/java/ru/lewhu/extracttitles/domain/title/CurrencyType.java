package ru.lewhu.extracttitles.domain.title;

public enum CurrencyType {
    VAULT,
    PLAYERPOINTS,
    NONE;

    public static CurrencyType fromString(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        try {
            return CurrencyType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
