package com.practice.observability_lab.memory.exception;

import com.practice.observability_lab.memory.MemoryController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = MemoryController.class)
public class MemoryExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidMemoryAllocationException.class)
    public Map<String, Object> handleInvalidAllocation(
            InvalidMemoryAllocationException exception
    ) {
        return Map.of(
                "error", "invalid_memory_allocation",
                "message", exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(MemoryLimitExceededException.class)
    public Map<String, Object> handleLimitExceeded(
            MemoryLimitExceededException exception
    ) {
        return Map.of(
                "error", "memory_limit_exceeded",
                "message", exception.getMessage(),
                "retainedBytes", exception.getRetainedBytes(),
                "requestedBytes", exception.getRequestedBytes(),
                "maxRetainedBytes", exception.getMaxRetainedBytes()
        );
    }
}
