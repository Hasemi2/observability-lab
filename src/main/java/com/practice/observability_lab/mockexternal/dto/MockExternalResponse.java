package com.practice.observability_lab.mockexternal.dto;

import java.time.Instant;

public record MockExternalResponse(
        String scenario,
        String message,
        long delayedMilliseconds,
        String threadName,
        Instant respondedAt
) {
}

