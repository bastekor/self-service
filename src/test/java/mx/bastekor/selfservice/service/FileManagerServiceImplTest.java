package mx.bastekor.selfservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import mx.bastekor.selfservice.enums.ErrorCode;
import mx.bastekor.selfservice.exception.BusinessException;
import mx.bastekor.selfservice.model.DirectoryContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileManagerServiceImplTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private io.micrometer.core.instrument.Counter counter;

    @TempDir
    Path tempDir;

    private FileManagerServiceImpl fileManagerService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        fileManagerService = new FileManagerServiceImpl(tempDir.toString(), meterRegistry);
    }

    @Test
    void createDirectory_success_returnsCreated() {
        ServiceResult<String> result = fileManagerService.createDirectory("docs");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getNotifications()).isNotEmpty();
    }

    @Test
    void createDirectory_pathTraversal_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.createDirectory(".."))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
    }

    @Test
    void createFile_success_returnsCreated() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                "hello".getBytes());

        ServiceResult<String> result = fileManagerService.createFile(file, "");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getNotifications()).isNotEmpty();
    }

    @Test
    void createFile_nullFile_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.createFile(null, ""))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MISSING_DOCUMENT);
    }

    @Test
    void createFile_emptyFilename_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", "hello".getBytes());
        
        assertThatThrownBy(() -> fileManagerService.createFile(file, ""))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MISSING_FILENAME);
    }

    @Test
    void getDirectoryContent_directoryNotExists_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.getDirectoryContent("nonexistent"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DIRECTORY_DOES_NOT_EXIST);
    }

    @Test
    void delete_pathTraversal_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.delete("file", "..", "test.txt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
    }

    @Test
    void delete_invalidType_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.delete("invalid", "", "test.txt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TYPE);
    }

    @Test
    void getFileContent_nullFilename_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.getFileContent("", null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NULL_OR_EMPTY_FILENAME);
    }

    @Test
    void updateDirectory_nullOldName_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.updateDirectory(null, "newName"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CURRENT_DIRECTORY_NAME_REQUIRED);
    }

    @Test
    void updateDirectory_nullNewName_throwsBusinessException() {
        assertThatThrownBy(() -> fileManagerService.updateDirectory("oldName", null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NEW_DIRECTORY_NAME_REQUIRED);
    }
}
