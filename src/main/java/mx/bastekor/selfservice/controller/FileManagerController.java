package mx.bastekor.selfservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import mx.bastekor.selfservice.model.ApiResponse;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import mx.bastekor.selfservice.service.FileContentResult;
import mx.bastekor.selfservice.service.FileManagerService;
import mx.bastekor.selfservice.service.ServiceResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static mx.bastekor.selfservice.constants.ApiPaths.BASE;
import static mx.bastekor.selfservice.constants.ApiPaths.CREATE_DIRECTORY;
import static mx.bastekor.selfservice.constants.ApiPaths.CREATE_FILE;
import static mx.bastekor.selfservice.constants.ApiPaths.DELETE_BY_TYPE;
import static mx.bastekor.selfservice.constants.ApiPaths.DIRECTORY_CONTENT;
import static mx.bastekor.selfservice.constants.ApiPaths.FILE_CONTENT;
import static mx.bastekor.selfservice.constants.ApiPaths.UPDATE_DIRECTORY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * REST controller for file system operations.
 */
@RestController
@AllArgsConstructor
@RequestMapping(BASE)
@Tag(name = "File manager", description = "Operaciones para directorios y archivos")
@Validated
public class FileManagerController {

    /** Service that encapsulates file system operations. */
    private final FileManagerService fileManagerService;

    /**
     * Lists files and folders for a given directory path.
     *
     * @param directory directory path to list.
     * @return response with files and folders metadata.
     */
    @GetMapping(DIRECTORY_CONTENT)
    @Operation(summary = "List directory content", description = "Regresa archivos y carpetas del directorio indicado.")
    public ResponseEntity<ApiResponse<DirectoryContentResponse>> getDirectoryContent(
            @Parameter(description = "Ruta del directorio a listar")
            @RequestParam(defaultValue = EMPTY) String directory) {
        ServiceResult<DirectoryContentResponse> result = fileManagerService.getDirectoryContent(directory);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    /**
     * Retrieves the content of a file as a downloadable resource.
     *
     * @param directory base directory path.
     * @param fileName  file name to retrieve.
     * @return file content or error payload.
     */
    @GetMapping(FILE_CONTENT)
    @Operation(summary = "Get file content", description = "Descarga el contenido de un archivo.")
    public ResponseEntity<?> getFileContent(
            @Parameter(description = "Ruta base del archivo")
            @RequestParam(defaultValue = EMPTY) String directory,
            @Parameter(description = "Nombre del archivo")
            @RequestParam @NotBlank String fileName) {
        FileContentResult result = fileManagerService.getFileContent(directory, fileName);
        if (!result.isSuccess()) {
            return ResponseEntity.status(result.getStatus()).body(result.getError());
        }
        return ResponseEntity.status(result.getStatus())
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .header(CONTENT_DISPOSITION, "attachment; filename\"" + result.getFilename() + "\"")
                .body(result.getResource());
    }

    /**
     * Creates a directory on the file system.
     *
     * @param directory directory path to create.
     * @return creation response.
     */
    @PostMapping(CREATE_DIRECTORY)
    @Operation(summary = "Create directory", description = "Crea un directorio en la ruta indicada.")
    public ResponseEntity<ApiResponse<String>> createDirectory(
            @RequestParam @NotBlank String directory) {
        ServiceResult<String> result = fileManagerService.createDirectory(directory);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    /**
     * Creates a file in the provided directory.
     *
     * @param file      file content.
     * @param directory directory where the file will be stored.
     * @return creation response.
     */
    @PostMapping(CREATE_FILE)
    @Operation(summary = "Create file", description = "Crea o reemplaza un archivo en el directorio indicado.")
    public ResponseEntity<ApiResponse<String>> createFile(
            @RequestPart @NotNull MultipartFile file,
            @RequestParam @NotBlank String directory) {
        ServiceResult<String> result = fileManagerService.createFile(file, directory);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    /**
     * Renames an existing directory.
     *
     * @param oldName current directory name.
     * @param newName new directory name.
     * @return update response.
     */
    @PutMapping(UPDATE_DIRECTORY)
    @Operation(summary = "Rename directory", description = "Renombra un directorio existente.")
    public ResponseEntity<ApiResponse<String>> update(
            @RequestParam @NotBlank String oldName,
            @RequestParam @NotBlank String newName) {
        ServiceResult<String> result = fileManagerService.updateDirectory(oldName, newName);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    /**
     * Deletes a file or directory by type.
     *
     * @param type      "file" or "directory".
     * @param directory base directory.
     * @param name      name of the entry to delete.
     * @return deletion response.
     */
    @DeleteMapping(DELETE_BY_TYPE)
    @Operation(summary = "Delete file or directory", description = "Elimina un archivo o directorio por tipo.")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable @NotBlank String type,
            @RequestParam @NotBlank String directory,
            @RequestParam @NotBlank String name) {
        ServiceResult<String> result = fileManagerService.delete(type, directory, name);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }
}
