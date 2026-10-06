package com.practice.observability_lab.dblock.dto;

public record DbLockResponse(
        String operation,
        long waitedMillis,
        long heldMillis,
        long value,
        String threadName
) {
}

