package com.practice.observability_lab.memory.exception;

public class InvalidMemoryAllocationException extends RuntimeException {

    public InvalidMemoryAllocationException(String message) {
        super(message);
    }
}
