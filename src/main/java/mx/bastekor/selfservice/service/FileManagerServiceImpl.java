package mx.bastekor.selfservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.selfservice.dto.FileSystemEntryDto;
import mx.bastekor.selfservice.dto.FileSystemEntryType;
import mx.bastekor.selfservice.enums.ErrorCode;
import mx.bastekor.selfservice.exception.BusinessException;
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

import static mx.bastekor.selfservice.util.ApiResponseFactory.success;
import static mx.bastekor.selfservice.util.ApiResponseFactory.withNotification;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.CREATED;

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
            incrementCounter("getDirectoryContent", "error");
            throw new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT, ex);
        }

        if (!Files.exists(path)) {
            log.error("Directory does not exist: {}", directory);
            incrementCounter("getDirectoryContent", "error");
            throw new BusinessException(ErrorCode.DIRECTORY_DOES_NOT_EXIST);
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
            incrementCounter("getDirectoryContent", "error");
            throw new BusinessException(ErrorCode.DIRECTORY_LISTED_ERROR, ioException);
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
            throw new BusinessException(ErrorCode.NULL_OR_EMPTY_FILENAME);
        }

        try {
            Path filePath = resolveSafePath(directory, fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                incrementCounter("getFileContent", "error");
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }

            if (!resource.isReadable()) {
                incrementCounter("getFileContent", "error");
                throw new BusinessException(ErrorCode.FILE_NO_READ_PERMISSIONS);
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            incrementCounter("getFileContent", "success");
            return FileContentResult.success(resource, contentType, resource.getFilename());
        } catch (SecurityException ex) {
            incrementCounter("getFileContent", "error");
            throw new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT, ex);
        } catch (IOException ioException) {
            incrementCounter("getFileContent", "error");
            throw new BusinessException(ErrorCode.ERROR_READING_FILE_CONTENT, ioException);
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
            throw new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT, ex);
        } catch (Exception exception) {
            log.error("Error creating directory, message :: {}", exception.getMessage(), exception);
            incrementCounter("createDirectory", "error");
            throw new BusinessException(ErrorCode.ERROR_CREATING_DIRECTORY, exception);
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
            throw new BusinessException(ErrorCode.MISSING_DOCUMENT);
        }

        if (isBlank(file.getOriginalFilename())) {
            incrementCounter("createFile", "error");
            throw new BusinessException(ErrorCode.MISSING_FILENAME);
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
            throw new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT, ex);
        } catch (Exception exception) {
            incrementCounter("createFile", "error");
            throw new BusinessException(ErrorCode.ERROR_CREATING_FILE, exception);
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
            throw new BusinessException(ErrorCode.CURRENT_DIRECTORY_NAME_REQUIRED);
        }

        if (isBlank(newName)) {
            incrementCounter("updateDirectory", "error");
            throw new BusinessException(ErrorCode.NEW_DIRECTORY_NAME_REQUIRED);
        }

        try {
            Path oldDir = resolveSafePath(oldName);
            Path newDir = resolveSafePath(newName);
            if (!Files.exists(oldDir) || !Files.isDirectory(oldDir)) {
                incrementCounter("updateDirectory", "error");
                throw new BusinessException(ErrorCode.DIRECTORY_TO_RENAME_NOT_FOUND);
            }
            if (Files.exists(newDir)) {
                incrementCounter("updateDirectory", "error");
                throw new BusinessException(ErrorCode.DIRECTORY_ALREADY_EXISTS);
            }
            Files.move(oldDir, newDir);
            ApiResponse<String> response = withNotification(null, "UPDATED",
                    "Directorio renombrado de '" + oldName + "' a '" + newName + "'");
            incrementCounter("updateDirectory", "success");
            return ServiceResult.of(response, HttpStatus.OK);
        } catch (SecurityException ex) {
            incrementCounter("updateDirectory", "error");
            throw new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT, ex);
        } catch (Exception ex) {
            log.error("Error renombrando directorio", ex);
            incrementCounter("updateDirectory", "error");
            throw new BusinessException(ErrorCode.ERROR_RENAMING_DIRECTORY, ex, ex.getMessage());
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

            boolean isFile = "file".equalsIgnoreCase(type);
            boolean isDirectory = "directory".equalsIgnoreCase(type);

            if (!isFile && !isDirectory) {
                incrementCounter("delete", "error");
                throw new BusinessException(ErrorCode.INVALID_TYPE);
            }

            if (!Files.exists(path)) {
                incrementCounter("delete", "error");
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND, type, name);
            }

            boolean deleted = false;
            if (isFile) {
                if (!Files.isRegularFile(path)) {
                    incrementCounter("delete", "error");
                    throw new BusinessException(ErrorCode.SPECIFIED_PATH_NOT_FILE);
                }
                deleted = Files.deleteIfExists(path);
            } else if (isDirectory) {
                if (!Files.isDirectory(path)) {
                    incrementCounter("delete", "error");
                    throw new BusinessException(ErrorCode.SPECIFIED_PATH_NOT_DIRECTORY);
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
            }

            if (!deleted) {
                incrementCounter("delete", "error");
                throw new BusinessException(ErrorCode.ERROR_DELETING, type);
            }

            incrementCounter("delete", "success");
            return ServiceResult.of(
                    withNotification(type + " eliminado: " + name, "DELETED",
                            type.substring(0, 1).toUpperCase() + type.substring(1) + " eliminado correctamente"),
                    HttpStatus.OK);

        } catch (SecurityException e) {
            log.error("Error de seguridad al eliminar {}: {}", type, name, e);
            incrementCounter("delete", "error");
            throw new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT, e, e.getMessage());
        } catch (IOException e) {
            log.error("Error de I/O al eliminar {}: {}", type, name, e);
            incrementCounter("delete", "error");
            throw new BusinessException(ErrorCode.ERROR_DELETING, e, type, e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al eliminar {}: {}", type, name, e);
            incrementCounter("delete", "error");
            throw new BusinessException(ErrorCode.UNEXPECTED_DELETE_ERROR, e, type, e.getMessage());
        }
    }
}
