package com.practice.observability_lab.dblock.exception;

import com.practice.observability_lab.dblock.DbLockController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = DbLockController.class)
public class DbLockExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDbLockRequestException.class)
    public Map<String, String> handleInvalidRequest(InvalidDbLockRequestException exception) {
        return Map.of(
                "error", "invalid_db_lock_request",
                "message", exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(DbLockAcquisitionException.class)
    public Map<String, String> handleAcquisitionFailure(DbLockAcquisitionException exception) {
        return Map.of(
                "error", "db_lock_acquisition_failed",
                "message", exception.getMessage()
        );
    }
}

