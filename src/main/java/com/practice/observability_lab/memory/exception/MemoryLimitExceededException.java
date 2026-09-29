package com.practice.observability_lab.memory.exception;

import lombok.Getter;

@Getter
public class MemoryLimitExceededException extends RuntimeException {

    private final long retainedBytes;
    private final long requestedBytes;
    private final long maxRetainedBytes;

    public MemoryLimitExceededException(
            long retainedBytes,
            long requestedBytes,
            long maxRetainedBytes
    ) {
        super("Memory retention limit would be exceeded");
        this.retainedBytes = retainedBytes;
        this.requestedBytes = requestedBytes;
        this.maxRetainedBytes = maxRetainedBytes;
    }
}
