package com.coffee.gu.response;


import com.coffee.gu.ErrorMessage;
import com.coffee.gu.ErrorType;

public record ApiResponse<T>(
        StatusType status,
        T data,
        ErrorMessage error
) {

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(StatusType.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(StatusType.SUCCESS, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorType errorType, Object errorData) {
        return new ApiResponse<>(StatusType.ERROR, null, ErrorMessage.of(errorType, errorData));
    }

}
