package mx.bastekor.selfservice.service;

import mx.bastekor.selfservice.model.DirectoryContentResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for file system operations.
 */
public interface FileManagerService {

    /**
     * Lists files and folders for a directory.
     *
     * @param directory directory path.
     * @return service result with content payload.
     */
    ServiceResult<DirectoryContentResponse> getDirectoryContent(String directory);

    /**
     * Reads a file and prepares it as a downloadable resource.
     *
     * @param directory base directory.
     * @param fileName  file name.
     * @return content result with resource or error.
     */
    FileContentResult getFileContent(String directory, String fileName);

    /**
     * Creates a directory.
     *
     * @param directory directory path.
     * @return service result with status.
     */
    ServiceResult<String> createDirectory(String directory);

    /**
     * Creates a file inside a directory.
     *
     * @param file      uploaded file.
     * @param directory destination directory.
     * @return service result with status.
     */
    ServiceResult<String> createFile(MultipartFile file, String directory);

    /**
     * Renames a directory.
     *
     * @param oldName current name.
     * @param newName new name.
     * @return service result with status.
     */
    ServiceResult<String> updateDirectory(String oldName, String newName);

    /**
     * Deletes a file or directory.
     *
     * @param type      "file" or "directory".
     * @param directory base directory.
     * @param name      entry name.
     * @return service result with status.
     */
    ServiceResult<String> delete(String type, String directory, String name);
}
