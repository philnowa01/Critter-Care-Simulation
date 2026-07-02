package com.crittercare.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateTimeUtils {

    private static final DateTimeFormatter DISPLAY_DATETIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TABLE_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private DateTimeUtils() {}

    public static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(DISPLAY_DATETIME);
    }

    public static String formatDate(LocalDate d) {
        return d == null ? "—" : d.format(DISPLAY_DATE);
    }

    public static String formatTableDateTime(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(TABLE_DATETIME);
    }

    public static String formatClock(LocalDateTime dt) {
        return dt == null ? "--:--:--" : dt.format(CLOCK);
    }

    /** Returns a human-readable relative age, e.g. "3h ago" or "2d ago". */
    public static String timeAgo(LocalDateTime dt) {
        if (dt == null) return "—";
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dt, now);
        if (minutes < 1)  return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = ChronoUnit.HOURS.between(dt, now);
        if (hours < 24)   return hours + "h ago";
        long days = ChronoUnit.DAYS.between(dt.toLocalDate(), now.toLocalDate());
        if (days < 7)     return days + "d ago";
        return dt.format(DISPLAY_DATE);
    }
}
