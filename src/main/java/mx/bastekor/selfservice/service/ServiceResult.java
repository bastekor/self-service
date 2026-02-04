package mx.bastekor.selfservice.service;

import lombok.Getter;
import mx.bastekor.selfservice.model.ApiResponse;
import org.springframework.http.HttpStatus;

/**
 * Wrapper for API response and HTTP status.
 *
 * @param <T> payload type.
 */
@Getter
public final class ServiceResult<T> {

    /** API response body. */
    private final ApiResponse<T> body;
    /** HTTP status to return. */
    private final HttpStatus status;

    private ServiceResult(ApiResponse<T> body, HttpStatus status) {
        this.body = body;
        this.status = status;
    }

    /**
     * Builds a service result wrapper.
     *
     * @param body   response body.
     * @param status http status.
     * @return service result.
     * @param <T> payload type.
     */
    public static <T> ServiceResult<T> of(ApiResponse<T> body, HttpStatus status) {
        return new ServiceResult<>(body, status);
    }
}
