package mx.bastekor.selfservice.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mx.bastekor.selfservice.model.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public final class FileContentResult {

    private final Resource resource;
    private final String contentType;
    private final String filename;
    private final HttpStatus status;
    private final ApiResponse<Void> error;

    public static FileContentResult success(Resource resource, String contentType, String filename) {
        return new FileContentResult(resource, contentType, filename, HttpStatus.OK, null);
    }

    public static FileContentResult error(ApiResponse<Void> error, HttpStatus status) {
        return new FileContentResult(null, null, null, status, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
}
