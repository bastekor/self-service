package mx.bastekor.selfservice.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.selfservice.model.Notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility helpers for notification creation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    /** Date time format used for notification timestamps. */
    public static final String FORMATTER_DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Builds a default success notification list.
     *
     * @return list with a single notification.
     */
    public static List<Notification> notificationList() {
        return notificationList("0", "Petición realizada con éxito");
    }

    /**
     * Builds a notification list for a given code and message.
     *
     * @param code    notification code.
     * @param message notification message.
     * @return list with a single notification.
     */
    public static List<Notification> notificationList(final String code, String message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMATTER_DATETIME);
        String formattedDateTime = now.format(formatter);
        return List.of(new Notification(code, message, formattedDateTime));
    }
}
