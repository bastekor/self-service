package mx.bastekor.selfservice.controller;

import lombok.extern.slf4j.Slf4j;
import mx.bastekor.selfservice.model.ApiResponse;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static mx.bastekor.selfservice.util.Utils.notificationList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/self-service")
public class FileManagerController {

    @GetMapping("/directory-content")
    public ResponseEntity<ApiResponse<DirectoryContentResponse>> getDirectoryContent(
            @RequestParam(defaultValue = EMPTY) String directory) {

        ApiResponse<DirectoryContentResponse> response = new ApiResponse<>();
        Path path = Paths.get(directory);

        if (!Files.exists(path)) {
            log.error("Directory does not exist: {}", directory);
            response.setNotifications(notificationList("DIRECTORY_LISTED", "No existe el directorio"));
            return ResponseEntity.badRequest().body(response);
        }

        DirectoryContentResponse directoryContentResponse = new DirectoryContentResponse();
        try (Stream<Path> stream = Files.list(path)) {
            stream.toList().forEach(path1 -> {
                String fileName = path1.getFileName().toString();
                if (path1.toFile().isDirectory()) {
                    directoryContentResponse.addFolder(fileName);
                } else {
                    directoryContentResponse.addFile(fileName);
                }
            });
            response.setData(directoryContentResponse);
            response.setNotifications(notificationList("DIRECTORY_LISTED", "Directorio listado correctamente"));
            return ResponseEntity.ok(response);
        } catch (IOException ioException) {
            log.error("Error reading directory content", ioException);
            response.setNotifications(notificationList("DIRECTORY_LISTED", "Error al listar el directorio: " + ioException.getMessage()));
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/file-content")
    public ResponseEntity<?> getFileContent(
            @RequestParam(defaultValue = EMPTY) String directory,
            @RequestParam String fileName) {

        if (fileName == null || fileName.trim().isEmpty()) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse<>(null, notificationList("400.1", "El nombre del archivo no puede ser nulo o vacío.")));
        }

        try {
            Path filePath = Paths.get(directory).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.status(400)
                        .body(new ApiResponse<>(null, notificationList("400.2", "El archivo o directorio no existe.")));
            }

            if (!resource.isReadable()) {
                return ResponseEntity.status(500)
                        .body(new ApiResponse<>(null, notificationList("500.1", "El archivo no tiene permisos de lectura.")));
            }

                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(CONTENT_DISPOSITION, "attachment; filename\"" + resource.getFilename() + "\"")
                        .body(resource);

        } catch (IOException ioException) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, notificationList("500.2", "Error leyendo el contenido del archivo.")));
        }
    }

    @PostMapping("/create-directory")
    public ResponseEntity<ApiResponse<String>> createDirectory(@RequestParam String directory) {

        try {
            Path path = Paths.get(directory);
            Files.createDirectory(path);
            return ResponseEntity.status(CREATED)
                    .body(new ApiResponse<>("Directorio creado o actualizado, según sea el caso", notificationList()));
        } catch (Exception exception) {
            log.error("Error creating directory, message :: {}", exception.getMessage(), exception);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, notificationList("500.1", "Error al crear el directorio")));
        }
    }

    @PostMapping("/create-file")
    public ResponseEntity<ApiResponse<String>> createFile(@RequestPart MultipartFile file,
                                                          @RequestParam String directory,
                                                          @RequestParam(required = false) String fileName) {

        if (file == null) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse<>(null, notificationList("400.1", "Sin documento.")));
        }

        if (isBlank(file.getOriginalFilename())) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse<>(null, notificationList("400.2", "No hay nombre para el documento.")));
        }

        try {
            byte[] bytes = file.getBytes();
            String tempFileName;
            if (isBlank(fileName)) {
                tempFileName = file.getOriginalFilename();
            } else {
                tempFileName = fileName;
            }

            Path path = Paths.get(directory).resolve(tempFileName);
            Files.write(path, bytes);
            return ResponseEntity.status(CREATED)
                    .body(new ApiResponse<>("Documento creado/actualizado, según sea el caso.", notificationList()));
        } catch (Exception exception) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, notificationList("500.1", "Error al crear el archivo.")));
        }
    }

    @PutMapping("/update-directory")
    public ResponseEntity<ApiResponse<String>> update(@RequestParam String oldName, @RequestParam String newName) {

        if (isBlank(newName)) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse<>(null, notificationList("400.1", "El actual nombre del directorio es requerido.")));
        }

        if (isBlank(newName)) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse<>(null, notificationList("400.2", "El nuevo nombre del directorio es requerido.")));
        }

        try {
            Path parent = Paths.get("").toAbsolutePath();
            Path oldDir = parent.resolve(oldName);
            Path newDir = parent.resolve(newName);
            if (!Files.exists(oldDir) || !Files.isDirectory(oldDir)) {
                return ResponseEntity.status(BAD_REQUEST)
                        .body(new ApiResponse<>(null, notificationList("400.3", "El directorio a renombrar no existe.")));
            }
            if (Files.exists(newDir)) {
                return ResponseEntity.status(BAD_REQUEST)
                        .body(new ApiResponse<>(null, notificationList("400.4", "Ya existe un directorio con el nuevo nombre.")));
            }
            Files.move(oldDir, newDir);
            ApiResponse<String> response = new ApiResponse<>(null, notificationList("UPDATED", "Directorio renombrado de '" + oldName + "' a '" + newName + "'"));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error renombrando directorio", ex);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, notificationList("500.2", "Error al renombrar el directorio: " + ex.getMessage())));
        }
    }

    @DeleteMapping("/delete/{type}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String type,
                                                      @RequestParam String directory,
                                                      @RequestParam String name) {
        try {
            Path path = Paths.get(directory).resolve(name);

            if (!Files.exists(path)) {
                return ResponseEntity.status(NOT_FOUND)
                        .body(new ApiResponse<>(null, notificationList("404.1", "No se encontró el " + type + ": " + name)));
            }

            boolean deleted;
            if ("file".equalsIgnoreCase(type)) {
                if (!Files.isRegularFile(path)) {
                    return ResponseEntity.status(BAD_REQUEST)
                            .body(new ApiResponse<>(null, notificationList("400.2", "La ruta especificada no es un archivo")));
                }
                deleted = Files.deleteIfExists(path);
            } else if ("directory".equalsIgnoreCase(type)) {
                if (!Files.isDirectory(path)) {
                    return ResponseEntity.status(BAD_REQUEST)
                            .body(new ApiResponse<>(null, notificationList("400.3", "La ruta especificada no es un directorio")));
                }
                // Eliminar directorio recursivamente
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.error("Error eliminando archivo: {}, MSG :: {}", p, e.getMessage(), e);
                                throw new RuntimeException(e);
                            }
                        });
                deleted = true;
            } else {
                return ResponseEntity.status(BAD_REQUEST)
                        .body(new ApiResponse<>(null, notificationList("400.4", "Tipo no válido. Debe ser 'file' o 'directory'")));
            }

            if (!deleted) {
                return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>(null, notificationList("500.1", "No se pudo eliminar el " + type)));
            }

            return ResponseEntity.ok(new ApiResponse<>(type + " eliminado: " + name,
                    notificationList("DELETED", type.substring(0, 1).toUpperCase() + type.substring(1) + " eliminado correctamente")));

        } catch (SecurityException e) {
            log.error("Error de seguridad al eliminar {}: {}", type, name, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, notificationList("403", "Acceso denegado: " + e.getMessage())));
        } catch (IOException e) {
            log.error("Error de I/O al eliminar {}: {}", type, name, e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, notificationList("500.2", "Error al eliminar " + type + ": " + e.getMessage())));
        } catch (Exception e) {
            log.error("Error inesperado al eliminar {}: {}", type, name, e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, notificationList("500.3", "Error inesperado al eliminar " + type + ": " + e.getMessage())));
        }
    }
}
