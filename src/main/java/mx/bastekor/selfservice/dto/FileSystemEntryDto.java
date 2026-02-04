package mx.bastekor.selfservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSystemEntryDto {

    private String name;
    private String path;
    private FileSystemEntryType type;
    private Long sizeBytes;
    private String extension;
    private String createdAt;
    private String lastModifiedAt;
    private String mimeType;
    private String owner;
    private String permissions;
    private boolean readable;
    private boolean writable;
    private boolean hidden;
}
