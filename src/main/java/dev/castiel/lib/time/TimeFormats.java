package dev.castiel.lib.time;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

public final class TimeFormats {
    private TimeFormats() {
    }

    public static String formatSeconds(long seconds) {
        return formatSeconds(seconds, false);
    }

    public static String formatSeconds(long seconds, boolean showZeroes) {
        long remaining = Math.max(0, seconds);
        long weeks = remaining / 604800L;
        remaining %= 604800L;
        long days = remaining / 86400L;
        remaining %= 86400L;
        long hours = remaining / 3600L;
        remaining %= 3600L;
        long minutes = remaining / 60L;
        remaining %= 60L;
        StringBuilder out = new StringBuilder();
        append(out, weeks, "w", showZeroes);
        append(out, days, "d", showZeroes);
        append(out, hours, "h", showZeroes);
        append(out, minutes, "m", showZeroes);
        append(out, remaining, "s", showZeroes || out.length() == 0);
        return out.toString();
    }

    public static long parseMillis(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0L;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return parseColonSeconds(normalized) * 1000L;
        }
        long totalSeconds = 0L;
        String[] parts = normalized.split("\\s+");
        for (String part : parts) {
            if (part.length() < 2) {
                continue;
            }
            char unit = part.charAt(part.length() - 1);
            long amount;
            try {
                amount = Long.parseLong(part.substring(0, part.length() - 1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (unit == 'w') totalSeconds += amount * 604800L;
            if (unit == 'd') totalSeconds += amount * 86400L;
            if (unit == 'h') totalSeconds += amount * 3600L;
            if (unit == 'm') totalSeconds += amount * 60L;
            if (unit == 's') totalSeconds += amount;
        }
        return totalSeconds * 1000L;
    }

    public static long secondsUntilDaily(String hhmm, ZoneId zoneId) {
        LocalTime target = LocalTime.parse(hhmm);
        ZoneId zone = zoneId == null ? ZoneId.systemDefault() : zoneId;
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime next = LocalDate.now(zone).atTime(target);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).getSeconds();
    }

    private static void append(StringBuilder out, long amount, String suffix, boolean include) {
        if (!include && amount <= 0) {
            return;
        }
        if (out.length() > 0) {
            out.append(' ');
        }
        out.append(amount).append(suffix);
    }

    private static long parseColonSeconds(String input) {
        String[] parts = input.split(":");
        long total = 0;
        for (String part : parts) {
            total *= 60;
            total += Long.parseLong(part);
        }
        return total;
    }
}
