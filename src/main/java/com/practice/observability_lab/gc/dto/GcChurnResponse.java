package com.practice.observability_lab.gc.dto;

public record GcChurnResponse(
        long requestedTotalMebibytes,
        int chunkKibibytes,
        long allocatedBytes,
        long allocationCount,
        long durationMillis,
        long checksum
) {
}
