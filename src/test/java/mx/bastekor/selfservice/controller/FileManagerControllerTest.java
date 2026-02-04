package mx.bastekor.selfservice.controller;

import mx.bastekor.selfservice.exception.ApiExceptionHandler;
import mx.bastekor.selfservice.model.ApiResponse;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import mx.bastekor.selfservice.service.FileManagerService;
import mx.bastekor.selfservice.service.ServiceResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static mx.bastekor.selfservice.util.ApiResponseFactory.withNotification;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
