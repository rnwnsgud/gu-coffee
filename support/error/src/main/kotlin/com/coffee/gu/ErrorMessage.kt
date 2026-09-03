package com.coffee.gu

class ErrorMessage(
    val code: ErrorCode,
    val message: String,
    val data: Any? = null
) {
    companion object {
        fun of(errorType: ErrorType, data: Any? = null): ErrorMessage {
            return ErrorMessage(
                code = errorType.code,
                message = errorType.message,
                data = data
            )
        }
    }
}
