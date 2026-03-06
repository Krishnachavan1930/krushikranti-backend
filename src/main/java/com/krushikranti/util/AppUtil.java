package com.krushikranti.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * General-purpose utility helpers for the KrushiKranti backend.
 */
public final class AppUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private AppUtil() {
        // Utility class — no instantiation
    }

    /**
     * Generates a unique order number in the format KK-{timestamp}-{random}.
     */
    public static String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "KK-" + timestamp + "-" + random;
    }

    /**
     * Formats a LocalDateTime to a readable string.
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Masks an email address for logging (e.g., te***@example.com).
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() > 2
                ? local.substring(0, 2) + "***"
                : "***";
        return masked + "@" + parts[1];
    }

    /**
     * Sanitizes a string to prevent XSS by removing HTML tags.
     */
    public static String sanitize(String input) {
        if (input == null)
            return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
