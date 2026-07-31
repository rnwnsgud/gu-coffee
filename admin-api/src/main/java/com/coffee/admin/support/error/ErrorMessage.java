package com.coffee.admin.support.error;

public record ErrorMessage(
        ErrorCode code,
        String message,
        Object data
) {
    public static ErrorMessage of(ErrorType errorType, Object data) {
        return new ErrorMessage(errorType.getCode(), errorType.getMessage(), data);
    }
}
