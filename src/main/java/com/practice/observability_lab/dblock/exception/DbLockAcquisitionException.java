package com.practice.observability_lab.dblock.exception;

public class DbLockAcquisitionException extends RuntimeException {

    public DbLockAcquisitionException(Throwable cause) {
        super("failed to acquire the database row lock within the configured timeout", cause);
    }
}

