package com.practice.observability_lab.gc.exception;

import com.practice.observability_lab.gc.GcController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = GcController.class)
public class GcExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidGcChurnRequestException.class)
    public Map<String, String> handleInvalidRequest(InvalidGcChurnRequestException exception) {
        return Map.of(
                "error", "invalid_gc_churn_request",
                "message", exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(GcChurnAlreadyRunningException.class)
    public Map<String, String> handleAlreadyRunning(GcChurnAlreadyRunningException exception) {
        return Map.of(
                "error", "gc_churn_already_running",
                "message", exception.getMessage()
        );
    }
}
