package com.practice.observability_lab.mockexternal.exception;

import com.practice.observability_lab.mockexternal.MockExternalController;
import com.practice.observability_lab.mockexternal.dto.MockExternalErrorResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Profile("external-api")
@RestControllerAdvice(basePackageClasses = MockExternalController.class)
public class MockExternalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidMockExternalRequestException.class)
    public MockExternalErrorResponse handleInvalidRequest(
            InvalidMockExternalRequestException exception
    ) {
        return new MockExternalErrorResponse(
                "invalid_mock_external_request",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
        );
    }

    @ExceptionHandler(MockExternalErrorException.class)
    public ResponseEntity<MockExternalErrorResponse> handleMockError(
            MockExternalErrorException exception
    ) {
        MockExternalErrorResponse response = new MockExternalErrorResponse(
                "mock_external_error",
                exception.getMessage(),
                exception.getStatus(),
                Instant.now()
        );

        return ResponseEntity.status(exception.getStatus()).body(response);
    }
}

