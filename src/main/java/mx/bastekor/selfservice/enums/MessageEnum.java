package mx.bastekor.selfservice.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Severity level for notifications or messages.
 */
@Schema(description = "Severidad del mensaje o notificacion.")
public enum MessageEnum {
    /** Informational message. */
    @Schema(description = "Mensaje informativo.")
    INFO,
    /** Warning message. */
    @Schema(description = "Mensaje de advertencia.")
    WARN,
    /** Error message. */
    @Schema(description = "Mensaje de error.")
    ERROR
}
