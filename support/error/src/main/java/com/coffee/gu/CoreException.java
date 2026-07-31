package com.coffee.gu;

public class CoreException extends RuntimeException{
    private ErrorType errorType;
    private Object data;

    public CoreException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Object getData() {
        return data;
    }
}
