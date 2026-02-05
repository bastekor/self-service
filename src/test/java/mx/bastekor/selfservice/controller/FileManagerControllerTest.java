package mx.bastekor.selfservice.controller;

import mx.bastekor.selfservice.exception.ApiExceptionHandler;
import mx.bastekor.selfservice.exception.BusinessException;
import mx.bastekor.selfservice.enums.ErrorCode;
import mx.bastekor.selfservice.model.ApiResponse;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import mx.bastekor.selfservice.service.FileManagerService;
import mx.bastekor.selfservice.service.ServiceResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static mx.bastekor.selfservice.util.ApiResponseFactory.withNotification;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.mock.mockito.MockBean;

@WebMvcTest(FileManagerController.class)
@Import(ApiExceptionHandler.class)
class FileManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileManagerService fileManagerService;

    @Test
    void createDirectory_blankParam_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/self-service/create-directory")
                        .param("directory", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.notifications[0].code").value("400.1"));
    }

    @Test
    void createDirectory_pathTraversal_returnsForbidden() throws Exception {
        when(fileManagerService.createDirectory(anyString()))
                .thenThrow(new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT));

        mockMvc.perform(post("/api/v1/self-service/create-directory")
                        .param("directory", ".."))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.notifications[0].code").value("403.1"));
    }

    @Test
    void getDirectoryContent_ok() throws Exception {
        ApiResponse<DirectoryContentResponse> body = withNotification(
                new DirectoryContentResponse(),
                "DIRECTORY_LISTED",
                "Directorio listado correctamente");
        ServiceResult<DirectoryContentResponse> result = ServiceResult.of(body, HttpStatus.OK);

        when(fileManagerService.getDirectoryContent("")).thenReturn(result);

        mockMvc.perform(get("/api/v1/self-service/directory-content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].code").value("DIRECTORY_LISTED"));
    }

    @Test
    void getDirectoryContent_notFound_returnsBadRequest() throws Exception {
        when(fileManagerService.getDirectoryContent(anyString()))
                .thenThrow(new BusinessException(ErrorCode.DIRECTORY_DOES_NOT_EXIST));

        mockMvc.perform(get("/api/v1/self-service/directory-content")
                        .param("directory", "nonexistent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.notifications[0].code").value("400.2"));
    }

    @Test
    void getFileContent_notFound_returnsBadRequest() throws Exception {
        when(fileManagerService.getFileContent(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/self-service/file-content")
                        .param("directory", "test")
                        .param("fileName", "nonexistent.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.notifications[0].code").value("400.2"));
    }

    @Test
    void createFile_success_returnsCreated() throws Exception {
        ApiResponse<String> response = withNotification("File created", "CREATED", "Success");
        ServiceResult<String> result = ServiceResult.of(response, HttpStatus.CREATED);

        when(fileManagerService.createFile(any(), anyString())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        mockMvc.perform(multipart("/api/v1/self-service/create-file")
                        .file(file)
                        .param("directory", "test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notifications[0].code").value("CREATED"));
    }

    @Test
    void updateDirectory_notFound_returnsBadRequest() throws Exception {
        when(fileManagerService.updateDirectory(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.DIRECTORY_TO_RENAME_NOT_FOUND));

        mockMvc.perform(put("/api/v1/self-service/update-directory")
                        .param("oldName", "nonexistent")
                        .param("newName", "newDir"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.notifications[0].code").value("400.3"));
    }

    @Test
    void delete_pathTraversal_returnsForbidden() throws Exception {
        when(fileManagerService.delete(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.PATH_OUTSIDE_ROOT));

        mockMvc.perform(delete("/api/v1/self-service/delete/file")
                        .param("directory", "..")
                        .param("name", "test.txt"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.notifications[0].code").value("403.1"));
    }
}
