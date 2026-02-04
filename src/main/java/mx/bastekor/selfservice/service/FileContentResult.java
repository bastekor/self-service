package mx.bastekor.selfservice.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mx.bastekor.selfservice.model.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;

/**
 * Response wrapper for file content requests.
 */
@Getter
@AllArgsConstructor
public final class FileContentResult {

    /** File resource to return in a download response. */
    private final Resource resource;
    /** MIME type for the response body. */
    private final String contentType;
    /** Original filename for the Content-Disposition header. */
    private final String filename;
    /** HTTP status to return. */
    private final HttpStatus status;
    /** Error body when the request fails. */
    private final ApiResponse<Void> error;

    /**
     * Builds a successful result with a downloadable resource.
     *
     * @param resource    file resource.
     * @param contentType MIME type.
     * @param filename    filename for download.
     * @return success result.
     */
    public static FileContentResult success(Resource resource, String contentType, String filename) {
        return new FileContentResult(resource, contentType, filename, HttpStatus.OK, null);
    }

    /**
     * Builds an error result with a payload.
     *
     * @param error  error response payload.
     * @param status http status to return.
     * @return error result.
     */
    public static FileContentResult error(ApiResponse<Void> error, HttpStatus status) {
        return new FileContentResult(null, null, null, status, error);
    }

    /**
     * Indicates if the result represents success.
     *
     * @return true when there is no error payload.
     */
    public boolean isSuccess() {
        return error == null;
    }
}
