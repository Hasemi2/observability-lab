package com.practice.observability_lab.dblock.dto;

public record DbLockStatusResponse(
        long value,
        int activeHolders,
        int waitingRequests
) {
}

