package com.coffee.gu.api.controller

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorLogLevel
import com.coffee.gu.ErrorType
import com.coffee.gu.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiControllerAdvice {
    companion object {
        private val log = LoggerFactory.getLogger(ApiControllerAdvice::class.java)
    }

    @ExceptionHandler(CoreException::class)
    fun handleCoreException(e: CoreException): ResponseEntity<ApiResponse<*>> {
        when (e.errorType.errorLogLevel) {
            ErrorLogLevel.ERROR -> log.error("com.coffee.gu.CoreException : {}", e.message, e)
            ErrorLogLevel.WARN -> log.warn("com.coffee.gu.CoreException : {}", e.message, e)
            else -> log.info("com.coffee.gu.CoreException : {}", e.message, e)
        }
        return ResponseEntity(ApiResponse.error<Any>(e.errorType, e.message), HttpStatusCode.valueOf(e.errorType.status))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<*>> {
        log.error("Exception : {}", e.message, e)
        return ResponseEntity(ApiResponse.error<Any>(ErrorType.DEFAULT_ERROR, e.message), HttpStatusCode.valueOf(ErrorType.DEFAULT_ERROR.status))
    }
}
