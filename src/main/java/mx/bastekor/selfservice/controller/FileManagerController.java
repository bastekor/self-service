package mx.bastekor.selfservice.controller;

import lombok.AllArgsConstructor;
import mx.bastekor.selfservice.model.ApiResponse;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import mx.bastekor.selfservice.service.FileContentResult;
import mx.bastekor.selfservice.service.FileManagerService;
import mx.bastekor.selfservice.service.ServiceResult;
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

import static mx.bastekor.selfservice.constants.ApiPaths.BASE;
import static mx.bastekor.selfservice.constants.ApiPaths.CREATE_DIRECTORY;
import static mx.bastekor.selfservice.constants.ApiPaths.CREATE_FILE;
import static mx.bastekor.selfservice.constants.ApiPaths.DELETE_BY_TYPE;
import static mx.bastekor.selfservice.constants.ApiPaths.DIRECTORY_CONTENT;
import static mx.bastekor.selfservice.constants.ApiPaths.FILE_CONTENT;
import static mx.bastekor.selfservice.constants.ApiPaths.UPDATE_DIRECTORY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@RestController
@AllArgsConstructor
@RequestMapping(BASE)
public class FileManagerController {

    private final FileManagerService fileManagerService;

    @GetMapping(DIRECTORY_CONTENT)
    public ResponseEntity<ApiResponse<DirectoryContentResponse>> getDirectoryContent(
            @RequestParam(defaultValue = EMPTY) String directory) {
        ServiceResult<DirectoryContentResponse> result = fileManagerService.getDirectoryContent(directory);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @GetMapping(FILE_CONTENT)
    public ResponseEntity<?> getFileContent(
            @RequestParam(defaultValue = EMPTY) String directory,
            @RequestParam String fileName) {
        FileContentResult result = fileManagerService.getFileContent(directory, fileName);
        if (!result.isSuccess()) {
            return ResponseEntity.status(result.getStatus()).body(result.getError());
        }
        return ResponseEntity.status(result.getStatus())
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .header(CONTENT_DISPOSITION, "attachment; filename\"" + result.getFilename() + "\"")
                .body(result.getResource());
    }

    @PostMapping(CREATE_DIRECTORY)
    public ResponseEntity<ApiResponse<String>> createDirectory(@RequestParam String directory) {
        ServiceResult<String> result = fileManagerService.createDirectory(directory);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @PostMapping(CREATE_FILE)
    public ResponseEntity<ApiResponse<String>> createFile(@RequestPart MultipartFile file,
                                                          @RequestParam String directory) {
        ServiceResult<String> result = fileManagerService.createFile(file, directory);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @PutMapping(UPDATE_DIRECTORY)
    public ResponseEntity<ApiResponse<String>> update(@RequestParam String oldName, @RequestParam String newName) {
        ServiceResult<String> result = fileManagerService.updateDirectory(oldName, newName);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @DeleteMapping(DELETE_BY_TYPE)
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String type,
                                                      @RequestParam String directory,
                                                      @RequestParam String name) {
        ServiceResult<String> result = fileManagerService.delete(type, directory, name);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }
}
