package mx.bastekor.selfservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Type of file system entry.
 */
@Schema(description = "Tipo de entrada en el sistema de archivos.")
public enum FileSystemEntryType {
    /** Regular file. */
    @Schema(description = "Archivo regular.")
    FILE,
    /** Directory or folder. */
    @Schema(description = "Directorio o carpeta.")
    DIRECTORY
}
