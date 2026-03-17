package ru.lewhu.extracttitles.util;

public final class TimeFormat {
    private TimeFormat() {}

    public static String remainingRussian(long millis) {
        return remaining(millis, "forever", "d.", "h.", "min.", "sec.");
    }

    public static String remaining(long millis,
                                   String foreverWord,
                                   String dayShort,
                                   String hourShort,
                                   String minuteShort,
                                   String secondShort) {
        if (millis == Long.MAX_VALUE) {
            return foreverWord;
        }
        if (millis <= 0) {
            return "0" + minuteShort;
        }

        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0) {
            return days + dayShort + " " + hours + hourShort + " " + minutes + minuteShort;
        }
        if (hours > 0) {
            return hours + hourShort + " " + minutes + minuteShort;
        }
        if (minutes > 0) {
            return minutes + minuteShort;
        }
        return seconds + secondShort;
    }
}
