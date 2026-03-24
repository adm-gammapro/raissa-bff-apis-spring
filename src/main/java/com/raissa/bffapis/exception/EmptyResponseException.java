package com.raissa.bffapis.exception;

public class EmptyResponseException extends RuntimeException {
    public EmptyResponseException(String message) {
        super(message);
    }
}
