package com.raissa.bffapis.exception;

public class ApiKeyValidationException extends RuntimeException {
    public ApiKeyValidationException(String message) {
        super(message);
    }

    public ApiKeyValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
