package mx.bastekor.selfservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard API response wrapper.
 *
 * @param <T> payload type.
 */
@Schema(description = "Respuesta estandar del API.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    /** Optional response payload. */
    @Schema(description = "Payload de la respuesta.")
    private T data;
    /** Messages describing the result of the request. */
    @Schema(description = "Notificaciones relacionadas con la respuesta.")
    private List<Notification> notifications;
}
