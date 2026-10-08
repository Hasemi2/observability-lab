package com.practice.observability_lab.mockexternal.exception;

public class InvalidMockExternalRequestException extends RuntimeException {

    public InvalidMockExternalRequestException(String message) {
        super(message);
    }
}

