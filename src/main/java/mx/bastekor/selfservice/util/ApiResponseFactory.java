package mx.bastekor.selfservice.util;

import mx.bastekor.selfservice.model.ApiResponse;

import static mx.bastekor.selfservice.util.Utils.notificationList;

/**
 * Factory for building standardized API responses.
 */
public final class ApiResponseFactory {

    private ApiResponseFactory() {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, notificationList());
    }

    public static <T> ApiResponse<T> withNotification(T data, String code, String message) {
        return new ApiResponse<>(data, notificationList(code, message));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(null, notificationList(code, message));
    }
}
