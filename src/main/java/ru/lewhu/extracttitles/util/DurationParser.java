package ru.lewhu.extracttitles.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static DurationResult parse(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("permanent")) {
            return new DurationResult(true, 0L);
        }

        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        Matcher matcher = TOKEN.matcher(normalized);
        long totalSeconds = 0;
        int consumed = 0;

        while (matcher.find()) {
            consumed += matcher.group(0).length();
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            totalSeconds += switch (unit) {
                case "s" -> value;
                case "m" -> value * 60;
                case "h" -> value * 3600;
                case "d" -> value * 86400;
                case "w" -> value * 604800;
                default -> 0;
            };
        }

        if (consumed != normalized.length() || totalSeconds <= 0) {
            return new DurationResult(false, -1L);
        }
        return new DurationResult(false, totalSeconds * 1000L);
    }

    public record DurationResult(boolean permanent, long millis) {
    }
}
