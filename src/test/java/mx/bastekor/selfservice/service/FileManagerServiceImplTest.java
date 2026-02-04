package mx.bastekor.selfservice.service;

import mx.bastekor.selfservice.model.DirectoryContentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileManagerServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void createDirectory_createsUnderRoot() {
        FileManagerServiceImpl service = new FileManagerServiceImpl(tempDir.toString());

        ServiceResult<String> result = service.createDirectory("docs");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(Files.exists(tempDir.resolve("docs"))).isTrue();
    }

    @Test
    void createFile_writesFile() throws IOException {
        FileManagerServiceImpl service = new FileManagerServiceImpl(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                "hello".getBytes());

        ServiceResult<String> result = service.createFile(file, "");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(Files.exists(tempDir.resolve("demo.txt"))).isTrue();
    }

    @Test
    void getDirectoryContent_listsFilesAndFolders() throws IOException {
        FileManagerServiceImpl service = new FileManagerServiceImpl(tempDir.toString());
        Files.createDirectory(tempDir.resolve("folder"));
        Files.writeString(tempDir.resolve("file.txt"), "content");

        ServiceResult<DirectoryContentResponse> result = service.getDirectoryContent("");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getData()).isNotNull();
        assertThat(result.getBody().getData().getFolders()).hasSize(1);
        assertThat(result.getBody().getData().getFiles()).hasSize(1);
    }

    @Test
    void pathTraversal_returnsForbidden() {
        FileManagerServiceImpl service = new FileManagerServiceImpl(tempDir.toString());

        ServiceResult<String> result = service.createDirectory("..");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
