package mx.bastekor.selfservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.bastekor.selfservice.dto.FileSystemEntryDto;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectoryContentResponse {
    private List<FileSystemEntryDto> files;
    private List<FileSystemEntryDto> folders;

    public void addFolder(FileSystemEntryDto folder) {
        if (this.folders == null) {
            this.folders = new ArrayList<>();
        }
        this.folders.add(folder);
    }

    public void addFile(FileSystemEntryDto file) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.add(file);
    }
}
