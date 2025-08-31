package mx.bastekor.selfservice.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.selfservice.model.Notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static final String FORMATTER_DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static List<Notification> notificationList() {
        return notificationList("0", "Petición realizada con éxito");
    }

    public static List<Notification> notificationList(final String code, String message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMATTER_DATETIME);
        String formattedDateTime = now.format(formatter);
        return List.of(new Notification(code, message, formattedDateTime));
    }
}