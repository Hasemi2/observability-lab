package com.practice.observability_lab.dblock.exception;

public class InvalidDbLockRequestException extends RuntimeException {

    public InvalidDbLockRequestException(String message) {
        super(message);
    }
}

