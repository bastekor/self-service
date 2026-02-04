package mx.bastekor.selfservice.service;

import mx.bastekor.selfservice.model.DirectoryContentResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagerService {

    ServiceResult<DirectoryContentResponse> getDirectoryContent(String directory);

    FileContentResult getFileContent(String directory, String fileName);

    ServiceResult<String> createDirectory(String directory);

    ServiceResult<String> createFile(MultipartFile file, String directory);

    ServiceResult<String> updateDirectory(String oldName, String newName);

    ServiceResult<String> delete(String type, String directory, String name);
}
