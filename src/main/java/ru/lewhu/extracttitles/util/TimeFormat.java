package ru.lewhu.extracttitles.util;

public final class TimeFormat {
    private TimeFormat() {}

    public static String remainingRussian(long millis) {
        if (millis == Long.MAX_VALUE) {
            return "\u043d\u0430\u0432\u0441\u0435\u0433\u0434\u0430";
        }
        if (millis <= 0) {
            return "0 \u043c\u0438\u043d.";
        }

        long totalMinutes = millis / 1000L / 60L;
        long days = totalMinutes / (60L * 24L);
        long hours = (totalMinutes % (60L * 24L)) / 60L;
        long minutes = totalMinutes % 60L;

        if (days > 0) {
            return days + "\u0434. " + hours + "\u0447. " + minutes + "\u043c\u0438\u043d.";
        }
        if (hours > 0) {
            return hours + "\u0447. " + minutes + "\u043c\u0438\u043d.";
        }
        return minutes + "\u043c\u0438\u043d.";
    }
}
