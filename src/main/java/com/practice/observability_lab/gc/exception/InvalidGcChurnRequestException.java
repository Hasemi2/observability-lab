package com.practice.observability_lab.gc.exception;

public class InvalidGcChurnRequestException extends RuntimeException {

    public InvalidGcChurnRequestException(String message) {
        super(message);
    }
}
