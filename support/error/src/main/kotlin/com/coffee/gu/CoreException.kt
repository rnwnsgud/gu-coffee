package com.coffee.gu

class CoreException(
    val errorType: ErrorType,
    val data: Any? = null
) : RuntimeException(errorType.message)