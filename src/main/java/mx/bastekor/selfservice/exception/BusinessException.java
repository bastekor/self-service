package mx.bastekor.selfservice.exception;

import mx.bastekor.selfservice.enums.ErrorCode;
import lombok.Getter;

/**
 * Business exception for application-specific errors.
 * Uses ErrorCode enum for consistent error handling.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Creates a business exception with error code and message arguments.
     *
     * @param errorCode the error code from enum
     * @param messageArgs optional arguments for message formatting
     */
    public BusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getFormattedMessage(messageArgs));
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Creates a business exception with error code and cause.
     *
     * @param errorCode the error code from enum
     * @param cause the underlying cause
     * @param messageArgs optional arguments for message formatting
     */
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.getFormattedMessage(messageArgs), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Gets the formatted message based on error code and arguments.
     *
     * @return formatted message
     */
    @Override
    public String getMessage() {
        return errorCode.getFormattedMessage(messageArgs);
    }

    /**
     * Gets the HTTP status code associated with this error.
     *
     * @return HTTP status
     */
    public int getHttpStatusCode() {
        return errorCode.getHttpStatus().value();
    }
}