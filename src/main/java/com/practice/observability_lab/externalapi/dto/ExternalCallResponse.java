package com.practice.observability_lab.externalapi.dto;

import com.practice.observability_lab.mockexternal.dto.MockExternalResponse;

import java.time.Instant;

public record ExternalCallResponse(
        String outcome,
        boolean fallback,
        long elapsedMilliseconds,
        MockExternalResponse externalResponse,
        String errorType,
        String errorMessage,
        Instant completedAt
) {

    public static ExternalCallResponse success(
            MockExternalResponse externalResponse,
            long elapsedMilliseconds
    ) {
        return new ExternalCallResponse(
                "SUCCESS",
                false,
                elapsedMilliseconds,
                externalResponse,
                null,
                null,
                Instant.now()
        );
    }

    public static ExternalCallResponse fallback(
            Throwable throwable,
            long elapsedMilliseconds
    ) {
        return new ExternalCallResponse(
                "FALLBACK",
                true,
                elapsedMilliseconds,
                null,
                throwable.getClass().getSimpleName(),
                throwable.getMessage(),
                Instant.now()
        );
    }
}
