package com.coffee.gu.api.controller;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {
    private static final Logger log = LoggerFactory.getLogger(ApiControllerAdvice.class);

    @ExceptionHandler(CoreException.class)
    ResponseEntity<ApiResponse<?>> handleCoreException(CoreException e) {
        switch (e.getErrorType().getErrorLogLevel()) {
            case ERROR -> log.error("com.coffee.gu.CoreException : {}", e.getMessage(), e);
            case WARN -> log.warn("com.coffee.gu.CoreException : {}", e.getMessage(), e);
            default -> log.info("com.coffee.gu.CoreException : {}", e.getMessage(), e);
        }
        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getMessage()), HttpStatusCode.valueOf(e.getErrorType().getStatus()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Exception : {}", e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error(ErrorType.DEFAULT_ERROR, e.getMessage()), HttpStatusCode.valueOf(ErrorType.DEFAULT_ERROR.getStatus()));
    }
}
