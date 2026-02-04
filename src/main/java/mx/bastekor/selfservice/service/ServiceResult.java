package mx.bastekor.selfservice.service;

import lombok.Getter;
import mx.bastekor.selfservice.model.ApiResponse;
import org.springframework.http.HttpStatus;

@Getter
public final class ServiceResult<T> {

    private final ApiResponse<T> body;
    private final HttpStatus status;

    private ServiceResult(ApiResponse<T> body, HttpStatus status) {
        this.body = body;
        this.status = status;
    }

    public static <T> ServiceResult<T> of(ApiResponse<T> body, HttpStatus status) {
        return new ServiceResult<>(body, status);
    }
}
