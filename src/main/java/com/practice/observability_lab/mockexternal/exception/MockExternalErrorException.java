package com.practice.observability_lab.mockexternal.exception;

import lombok.Getter;

@Getter
public class MockExternalErrorException extends RuntimeException {

    private final int status;

    public MockExternalErrorException(int status) {
        super("mock external API returned HTTP " + status);
        this.status = status;
    }
}

