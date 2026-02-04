package mx.bastekor.selfservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for a file system entry used in API responses.
 */
@Schema(description = "Metadatos de un archivo o directorio.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSystemEntryDto {

    /** Entry name without path. */
    @Schema(description = "Nombre del archivo o carpeta.")
    private String name;
    /** Absolute path for the entry. */
    @Schema(description = "Ruta absoluta del recurso.")
    private String path;
    /** Entry type (file or directory). */
    @Schema(description = "Tipo de recurso.", implementation = FileSystemEntryType.class)
    private FileSystemEntryType type;
    /** File size in bytes, if applicable. */
    @Schema(description = "Tamano en bytes si aplica.")
    private Long sizeBytes;
    /** File extension without dot, if applicable. */
    @Schema(description = "Extension del archivo sin el punto.")
    private String extension;
    /** Creation timestamp formatted as ISO string. */
    @Schema(description = "Fecha de creacion en formato ISO.")
    private String createdAt;
    /** Last modification timestamp formatted as ISO string. */
    @Schema(description = "Fecha de ultima modificacion en formato ISO.")
    private String lastModifiedAt;
    /** Detected MIME type for files. */
    @Schema(description = "Tipo MIME detectado.")
    private String mimeType;
    /** File owner name, when available. */
    @Schema(description = "Propietario del archivo si esta disponible.")
    private String owner;
    /** POSIX style permissions, when available. */
    @Schema(description = "Permisos POSIX si estan disponibles.")
    private String permissions;
    /** Whether the entry is readable. */
    @Schema(description = "Indica si es legible.")
    private boolean readable;
    /** Whether the entry is writable. */
    @Schema(description = "Indica si es escribible.")
    private boolean writable;
    /** Whether the entry is hidden. */
    @Schema(description = "Indica si esta oculto.")
    private boolean hidden;
}
