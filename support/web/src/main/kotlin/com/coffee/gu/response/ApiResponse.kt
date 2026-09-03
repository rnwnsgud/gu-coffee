package com.coffee.gu.response

import com.coffee.gu.ErrorMessage
import com.coffee.gu.ErrorType

class ApiResponse<T>(
    val status: StatusType,
    val data: T? = null,
    val error: ErrorMessage? = null
) {
    companion object {
        fun <T> success(): ApiResponse<T> = ApiResponse(StatusType.SUCCESS)

        fun <T> success(data: T): ApiResponse<T> = ApiResponse(StatusType.SUCCESS, data)

        fun <T> error(errorType: ErrorType, errorData: Any? = null): ApiResponse<T> =
            ApiResponse(StatusType.ERROR, error = ErrorMessage.of(errorType, errorData))
    }
}
