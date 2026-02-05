package mx.bastekor.selfservice.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for the application.
 * Provides consistent error messaging and HTTP status mapping.
 */
@Schema(description = "Códigos de error centralizados del sistema.")
public enum ErrorCode {

    // 400.x - Client Validation Errors
    VALIDATION_FAILED("400.1", "Validación fallida", HttpStatus.BAD_REQUEST),
    MISSING_DOCUMENT("400.1", "Sin documento", HttpStatus.BAD_REQUEST),
    NULL_OR_EMPTY_FILENAME("400.1", "El nombre del archivo no puede ser nulo o vacio", HttpStatus.BAD_REQUEST),
    CURRENT_DIRECTORY_NAME_REQUIRED("400.1", "El actual nombre del directorio es requerido", HttpStatus.BAD_REQUEST),
    
    FILE_NOT_FOUND("400.2", "El archivo o directorio no existe", HttpStatus.BAD_REQUEST),
    MISSING_FILENAME("400.2", "No hay nombre para el documento", HttpStatus.BAD_REQUEST),
    NEW_DIRECTORY_NAME_REQUIRED("400.2", "El nuevo nombre del directorio es requerido", HttpStatus.BAD_REQUEST),
    
    DIRECTORY_TO_RENAME_NOT_FOUND("400.3", "El directorio a renombrar no existe", HttpStatus.BAD_REQUEST),
    SPECIFIED_PATH_NOT_FILE("400.2", "La ruta especificada no es un archivo", HttpStatus.BAD_REQUEST),
    SPECIFIED_PATH_NOT_DIRECTORY("400.3", "La ruta especificada no es un directorio", HttpStatus.BAD_REQUEST),
    
    DIRECTORY_ALREADY_EXISTS("400.4", "Ya existe un directorio con el nuevo nombre", HttpStatus.BAD_REQUEST),
    INVALID_TYPE("400.4", "Tipo no valido. Debe ser 'file' o 'directory'", HttpStatus.BAD_REQUEST),
    DIRECTORY_DOES_NOT_EXIST("400.2", "No existe el directorio", HttpStatus.BAD_REQUEST),

    // 403.x - Forbidden/Access Denied Errors
    PATH_OUTSIDE_ROOT("403.1", "Ruta fuera del directorio permitido", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("403.1", "Acceso denegado", HttpStatus.FORBIDDEN),

    // 500.x - Internal Server Errors
    UNEXPECTED_ERROR("500.0", "Error inesperado", HttpStatus.INTERNAL_SERVER_ERROR),
    IO_ERROR("500.1", "Error de I/O", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NO_READ_PERMISSIONS("500.1", "El archivo no tiene permisos de lectura", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_READING_FILE_CONTENT("500.2", "Error leyendo el contenido del archivo", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_CREATING_DIRECTORY("500.1", "Error al crear el directorio", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_CREATING_FILE("500.1", "Error al crear el archivo", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_RENAMING_DIRECTORY("500.2", "Error al renombrar el directorio", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_DELETING("500.2", "Error al eliminar", HttpStatus.INTERNAL_SERVER_ERROR),
    UNEXPECTED_DELETE_ERROR("500.3", "Error inesperado al eliminar", HttpStatus.INTERNAL_SERVER_ERROR),

    // Success/Info Codes
    DIRECTORY_LISTED("DIRECTORY_LISTED", "Directorio listado correctamente", HttpStatus.OK),
    DIRECTORY_LISTED_ERROR("DIRECTORY_LISTED", "Error al listar el directorio", HttpStatus.BAD_REQUEST),
    UPDATED("UPDATED", "Directorio renombrado", HttpStatus.OK),
    DELETED("DELETED", "Elemento eliminado correctamente", HttpStatus.OK);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Gets the formatted message with optional arguments.
     * @param args arguments to format into the message
     * @return formatted message
     */
    public String getFormattedMessage(Object... args) {
        if (args == null || args.length == 0) {
            return defaultMessage;
        }
        return String.format(defaultMessage, args);
    }
}