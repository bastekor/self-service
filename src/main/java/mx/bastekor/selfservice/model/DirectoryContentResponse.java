package mx.bastekor.selfservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectoryContentResponse {
    private List<String> files;
    private List<String> folders;

    public void addFolder(String folder) {
        if (this.folders == null) {
            this.folders = new ArrayList<>();
        }
        this.folders.add(folder);
    }

    public void addFile(String file) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.add(file);
    }
}