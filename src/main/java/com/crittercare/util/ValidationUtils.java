package com.crittercare.util;

public final class ValidationUtils {

    private ValidationUtils() {}

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean isValidAge(String s) {
        if (s == null) return false;
        try {
            return Integer.parseInt(s.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPercent(String s) {
        if (s == null) return false;
        try {
            double v = Double.parseDouble(s.trim());
            return v >= 0 && v <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidCapacity(String s) {
        if (s == null) return false;
        try {
            return Integer.parseInt(s.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Returns the trimmed string, or null if blank. */
    public static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Returns the trimmed string, or the default value if blank. */
    public static String trimOrDefault(String s, String defaultValue) {
        return (s == null || s.isBlank()) ? defaultValue : s.trim();
    }
}
