package mx.bastekor.selfservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.bastekor.selfservice.dto.FileSystemEntryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response payload for directory listing.
 */
@Schema(description = "Contenido de un directorio agrupado por archivos y carpetas.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectoryContentResponse {
    /** List of files inside the directory. */
    @Schema(description = "Lista de archivos del directorio.")
    private List<FileSystemEntryDto> files;
    /** List of subfolders inside the directory. */
    @Schema(description = "Lista de carpetas del directorio.")
    private List<FileSystemEntryDto> folders;

    /**
     * Adds a folder entry and initializes the list if needed.
     *
     * @param folder folder metadata.
     */
    public void addFolder(FileSystemEntryDto folder) {
        if (this.folders == null) {
            this.folders = new ArrayList<>();
        }
        this.folders.add(folder);
    }

    /**
     * Adds a file entry and initializes the list if needed.
     *
     * @param file file metadata.
     */
    public void addFile(FileSystemEntryDto file) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.add(file);
    }
}
