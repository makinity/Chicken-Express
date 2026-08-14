package com.chickenexpress.foodorder.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payload sent over WebSocket to admin and customer clients.
 *
 * <p>Jackson serialises this to JSON automatically.
 * All fields are plain strings so the JS client needs no special parsing.</p>
 *
 * Example JSON:
 * <pre>
 * {
 *   "type":    "NEW_ORDER",
 *   "title":   "New Order",
 *   "message": "CE-20260814-0012 by Maki — ₱450.00",
 *   "link":    "/admin/orders/42",
 *   "icon":    "bi-receipt",
 *   "at":      "11:06 AM"
 * }
 * </pre>
 */
public record NotificationPayload(
        String type,
        String title,
        String message,
        String link,
        String icon,
        String at
) {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a");

    /** Convenience factory — stamps current time automatically. */
    public static NotificationPayload of(String type, String title,
                                          String message, String link, String icon) {
        return new NotificationPayload(
                type, title, message, link, icon,
                LocalDateTime.now().format(TIME_FMT)
        );
    }
}
