package mx.bastekor.selfservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification entry used in API responses.
 */
@Schema(description = "Notificacion de respuesta del API.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    /** Notification code for client handling. */
    @Schema(description = "Codigo de la notificacion.")
    private String code;
    /** Human-readable message. */
    @Schema(description = "Descripcion legible para el usuario.")
    private String description;
    /** Timestamp when the notification was created. */
    @Schema(description = "Marca de tiempo de la notificacion.")
    private String timestamp;
}
