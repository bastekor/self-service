package mx.bastekor.selfservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.selfservice.dto.FileSystemEntryDto;
import mx.bastekor.selfservice.dto.FileSystemEntryType;
import mx.bastekor.selfservice.model.ApiResponse;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import static mx.bastekor.selfservice.util.ApiResponseFactory.error;
import static mx.bastekor.selfservice.util.ApiResponseFactory.success;
import static mx.bastekor.selfservice.util.ApiResponseFactory.withNotification;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Default implementation for file system operations.
 */
@Slf4j
@Service
public class FileManagerServiceImpl implements FileManagerService {

    private final Path rootPath;
    private final MeterRegistry meterRegistry;

    @Autowired
    public FileManagerServiceImpl(@Value("${app.fs.root}") String rootDirectory, MeterRegistry meterRegistry) {
        this.rootPath = Paths.get(rootDirectory).toAbsolutePath().normalize();
        this.meterRegistry = meterRegistry;
    }

    /**
     * Lists directory content and builds metadata for each entry.
     *
     * @param directory directory path.
     * @return result with directory content or error notifications.
     */
    @Override
    public ServiceResult<DirectoryContentResponse> getDirectoryContent(String directory) {
        log.info("Listing directory content. directory={}", directory);
        ApiResponse<DirectoryContentResponse> response = new ApiResponse<>();
        Path path;
        try {
            path = resolveSafePath(directory);
        } catch (SecurityException ex) {
            log.warn("Ruta fuera del root permitido: {}", directory);
            response = withNotification(null, "403.1", "Ruta fuera del directorio permitido.");
            incrementCounter("getDirectoryContent", "error");
            return ServiceResult.of(response, FORBIDDEN);
        }

        if (!Files.exists(path)) {
            log.error("Directory does not exist: {}", directory);
            response = withNotification(null, "DIRECTORY_LISTED", "No existe el directorio");
            incrementCounter("getDirectoryContent", "error");
            return ServiceResult.of(response, BAD_REQUEST);
        }

        DirectoryContentResponse directoryContentResponse = new DirectoryContentResponse();
        try (Stream<Path> stream = Files.list(path)) {
            stream.toList().forEach(path1 -> {
                if (path1.toFile().isDirectory()) {
                    directoryContentResponse.addFolder(buildEntry(path1, FileSystemEntryType.DIRECTORY));
                } else {
                    directoryContentResponse.addFile(buildEntry(path1, FileSystemEntryType.FILE));
                }
            });
            response = withNotification(directoryContentResponse, "DIRECTORY_LISTED", "Directorio listado correctamente");
            incrementCounter("getDirectoryContent", "success");
            return ServiceResult.of(response, HttpStatus.OK);
        } catch (IOException ioException) {
            log.error("Error reading directory content", ioException);
            response = withNotification(null, "DIRECTORY_LISTED", "Error al listar el directorio: " + ioException.getMessage());
            incrementCounter("getDirectoryContent", "error");
            return ServiceResult.of(response, INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Builds metadata for a file system entry.
     *
     * @param path entry path.
     * @param type entry type.
     * @return populated DTO.
     */
    private FileSystemEntryDto buildEntry(Path path, FileSystemEntryType type) {
        String name = path.getFileName().toString();
        String extension = FileSystemEntryType.FILE.equals(type) ? getExtension(name) : null;
        Long sizeBytes = null;
        String createdAt = null;
        String lastModifiedAt = null;
        String mimeType = null;
        String owner = null;
        String permissions = null;
        boolean readable = false;
        boolean writable = false;
        boolean hidden = false;

        try {
            if (FileSystemEntryType.FILE.equals(type)) {
                sizeBytes = Files.size(path);
                mimeType = Files.probeContentType(path);
            }
            FileTime creationTime = (FileTime) Files.getAttribute(path, "basic:creationTime");
            createdAt = formatFileTime(creationTime);
            lastModifiedAt = formatFileTime(Files.getLastModifiedTime(path));
            owner = Files.getOwner(path).getName();
            permissions = formatPermissions(path);
            readable = Files.isReadable(path);
            writable = Files.isWritable(path);
            hidden = Files.isHidden(path);
        } catch (IOException ioException) {
            log.warn("No fue posible leer metadatos para {}", path, ioException);
        }

        return new FileSystemEntryDto(
                name,
                path.toAbsolutePath().toString(),
                type,
                sizeBytes,
                extension,
                createdAt,
                lastModifiedAt,
                mimeType,
                owner,
                permissions,
                readable,
                writable,
                hidden);
    }

    /**
     * Formats a file timestamp to ISO-8601 with offset.
     *
     * @param fileTime time to format.
     * @return formatted time or null.
     */
    private String formatFileTime(FileTime fileTime) {
        if (fileTime == null) {
            return null;
        }
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                fileTime.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * Formats POSIX permissions if available.
     *
     * @param path path to read.
     * @return POSIX permissions or null when unsupported.
     */
    private String formatPermissions(Path path) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            return PosixFilePermissions.toString(perms);
        } catch (UnsupportedOperationException | IOException ex) {
            return null;
        }
    }

    /**
     * Extracts the extension from a file name.
     *
     * @param name file name.
     * @return extension without dot or null.
     */
    private String getExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index <= 0 || index == name.length() - 1) {
            return null;
        }
        return name.substring(index + 1);
    }

    /**
     * Resolves a directory path safely against the configured root.
     *
     * @param directory directory path, relative to root.
     * @return normalized safe path.
     */
    private Path resolveSafePath(String directory) {
        Path resolved = isBlank(directory) ? rootPath : rootPath.resolve(directory).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new SecurityException("Ruta fuera del root permitido.");
        }
        return resolved;
    }

    /**
     * Resolves a file path safely against the configured root.
     *
     * @param directory base directory relative to root.
     * @param name      file or directory name.
     * @return normalized safe path.
     */
    private Path resolveSafePath(String directory, String name) {
        Path resolved = resolveSafePath(directory).resolve(name).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new SecurityException("Ruta fuera del root permitido.");
        }
        return resolved;
    }

    /**
     * Increments an operation counter for observability.
     *
     * @param operation operation name.
     * @param result    result label (success or error).
     */
    private void incrementCounter(String operation, String result) {
        meterRegistry.counter("fs.operation", "name", operation, "result", result).increment();
    }

    /**
     * Reads a file as a Spring Resource.
     *
     * @param directory base directory.
     * @param fileName  file name.
     * @return file content result.
     */
    @Override
    public FileContentResult getFileContent(String directory, String fileName) {
        log.info("Getting file content. directory={}, fileName={}", directory, fileName);
        if (fileName == null || fileName.trim().isEmpty()) {
            incrementCounter("getFileContent", "error");
            return FileContentResult.error(
                    error("400.1", "El nombre del archivo no puede ser nulo o vacio."),
                    BAD_REQUEST);
        }

        try {
            Path filePath = resolveSafePath(directory, fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                incrementCounter("getFileContent", "error");
                return FileContentResult.error(
                    error("400.2", "El archivo o directorio no existe."),
                    BAD_REQUEST);
            }

            if (!resource.isReadable()) {
                incrementCounter("getFileContent", "error");
                return FileContentResult.error(
                        error("500.1", "El archivo no tiene permisos de lectura."),
                        INTERNAL_SERVER_ERROR);
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            incrementCounter("getFileContent", "success");
            return FileContentResult.success(resource, contentType, resource.getFilename());
        } catch (SecurityException ex) {
            incrementCounter("getFileContent", "error");
            return FileContentResult.error(
                    error("403.1", "Ruta fuera del directorio permitido."),
                    FORBIDDEN);
        } catch (IOException ioException) {
            incrementCounter("getFileContent", "error");
            return FileContentResult.error(
                    error("500.2", "Error leyendo el contenido del archivo."),
                    INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates a new directory.
     *
     * @param directory directory path.
     * @return service result with status.
     */
    @Override
    public ServiceResult<String> createDirectory(String directory) {
        log.info("Creating directory. directory={}", directory);
        try {
            Path path = resolveSafePath(directory);
            Files.createDirectory(path);
            incrementCounter("createDirectory", "success");
            return ServiceResult.of(
                    success("Directorio creado o actualizado, segun sea el caso"),
                    CREATED);
        } catch (SecurityException ex) {
            incrementCounter("createDirectory", "error");
            return ServiceResult.of(
                    error("403.1", "Ruta fuera del directorio permitido."),
                    FORBIDDEN);
        } catch (Exception exception) {
            log.error("Error creating directory, message :: {}", exception.getMessage(), exception);
            incrementCounter("createDirectory", "error");
            return ServiceResult.of(
                    error("500.1", "Error al crear el directorio"),
                    INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates or replaces a file.
     *
     * @param file      file content.
     * @param directory destination directory.
     * @return service result with status.
     */
    @Override
    public ServiceResult<String> createFile(MultipartFile file, String directory) {
        log.info("Creating file. directory={}, originalName={}", directory, file != null ? file.getOriginalFilename() : null);
        if (file == null) {
            incrementCounter("createFile", "error");
            return ServiceResult.of(
                    error("400.1", "Sin documento."),
                    BAD_REQUEST);
        }

        if (isBlank(file.getOriginalFilename())) {
            incrementCounter("createFile", "error");
            return ServiceResult.of(
                    error("400.2", "No hay nombre para el documento."),
                    BAD_REQUEST);
        }

        try {
            byte[] bytes = file.getBytes();
            Path path = resolveSafePath(directory, file.getOriginalFilename());
            Files.write(path, bytes);
            incrementCounter("createFile", "success");
            return ServiceResult.of(
                    success("Documento creado/actualizado, segun sea el caso."),
                    CREATED);
        } catch (SecurityException ex) {
            incrementCounter("createFile", "error");
            return ServiceResult.of(
                    error("403.1", "Ruta fuera del directorio permitido."),
                    FORBIDDEN);
        } catch (Exception exception) {
            incrementCounter("createFile", "error");
            return ServiceResult.of(
                    error("500.1", "Error al crear el archivo."),
                    INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Renames a directory.
     *
     * @param oldName current directory name.
     * @param newName new directory name.
     * @return service result with status.
     */
    @Override
    public ServiceResult<String> updateDirectory(String oldName, String newName) {
        log.info("Renaming directory. oldName={}, newName={}", oldName, newName);
        if (isBlank(oldName)) {
            incrementCounter("updateDirectory", "error");
            return ServiceResult.of(
                    error("400.1", "El actual nombre del directorio es requerido."),
                    BAD_REQUEST);
        }

        if (isBlank(newName)) {
            incrementCounter("updateDirectory", "error");
            return ServiceResult.of(
                    error("400.2", "El nuevo nombre del directorio es requerido."),
                    BAD_REQUEST);
        }

        try {
            Path oldDir = resolveSafePath(oldName);
            Path newDir = resolveSafePath(newName);
            if (!Files.exists(oldDir) || !Files.isDirectory(oldDir)) {
                incrementCounter("updateDirectory", "error");
                return ServiceResult.of(
                        error("400.3", "El directorio a renombrar no existe."),
                        BAD_REQUEST);
            }
            if (Files.exists(newDir)) {
                incrementCounter("updateDirectory", "error");
                return ServiceResult.of(
                        error("400.4", "Ya existe un directorio con el nuevo nombre."),
                        BAD_REQUEST);
            }
            Files.move(oldDir, newDir);
            ApiResponse<String> response = withNotification(null, "UPDATED",
                    "Directorio renombrado de '" + oldName + "' a '" + newName + "'");
            incrementCounter("updateDirectory", "success");
            return ServiceResult.of(response, HttpStatus.OK);
        } catch (SecurityException ex) {
            incrementCounter("updateDirectory", "error");
            return ServiceResult.of(
                    error("403.1", "Ruta fuera del directorio permitido."),
                    FORBIDDEN);
        } catch (Exception ex) {
            log.error("Error renombrando directorio", ex);
            incrementCounter("updateDirectory", "error");
            return ServiceResult.of(
                    error("500.2", "Error al renombrar el directorio: " + ex.getMessage()),
                    INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a file or directory.
     *
     * @param type      "file" or "directory".
     * @param directory base directory.
     * @param name      entry name.
     * @return service result with status.
     */
    @Override
    public ServiceResult<String> delete(String type, String directory, String name) {
        log.info("Deleting entry. type={}, directory={}, name={}", type, directory, name);
        try {
            Path path = resolveSafePath(directory, name);

            if (!Files.exists(path)) {
                incrementCounter("delete", "error");
                return ServiceResult.of(
                        error("404.1", "No se encontro el " + type + ": " + name),
                        NOT_FOUND);
            }

            boolean deleted;
            if ("file".equalsIgnoreCase(type)) {
                if (!Files.isRegularFile(path)) {
                    incrementCounter("delete", "error");
                    return ServiceResult.of(
                            error("400.2", "La ruta especificada no es un archivo"),
                            BAD_REQUEST);
                }
                deleted = Files.deleteIfExists(path);
            } else if ("directory".equalsIgnoreCase(type)) {
                if (!Files.isDirectory(path)) {
                    incrementCounter("delete", "error");
                    return ServiceResult.of(
                            error("400.3", "La ruta especificada no es un directorio"),
                            BAD_REQUEST);
                }
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
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
                incrementCounter("delete", "error");
                return ServiceResult.of(
                        error("400.4", "Tipo no valido. Debe ser 'file' o 'directory'"),
                        BAD_REQUEST);
            }

            if (!deleted) {
                incrementCounter("delete", "error");
                return ServiceResult.of(
                        error("500.1", "No se pudo eliminar el " + type),
                        INTERNAL_SERVER_ERROR);
            }

            incrementCounter("delete", "success");
            return ServiceResult.of(
                    withNotification(type + " eliminado: " + name, "DELETED",
                            type.substring(0, 1).toUpperCase() + type.substring(1) + " eliminado correctamente"),
                    HttpStatus.OK);

        } catch (SecurityException e) {
            log.error("Error de seguridad al eliminar {}: {}", type, name, e);
            incrementCounter("delete", "error");
            return ServiceResult.of(
                    error("403", "Acceso denegado: " + e.getMessage()),
                    HttpStatus.FORBIDDEN);
        } catch (IOException e) {
            log.error("Error de I/O al eliminar {}: {}", type, name, e);
            incrementCounter("delete", "error");
            return ServiceResult.of(
                    error("500.2", "Error al eliminar " + type + ": " + e.getMessage()),
                    INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Error inesperado al eliminar {}: {}", type, name, e);
            incrementCounter("delete", "error");
            return ServiceResult.of(
                    error("500.3", "Error inesperado al eliminar " + type + ": " + e.getMessage()),
                    INTERNAL_SERVER_ERROR);
        }
    }
}
