package com.practice.observability_lab.mockexternal.dto;

import java.time.Instant;

public record MockExternalErrorResponse(
        String error,
        String message,
        int status,
        Instant respondedAt
) {
}

