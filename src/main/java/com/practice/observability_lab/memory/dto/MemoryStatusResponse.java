package com.practice.observability_lab.memory.dto;

public record MemoryStatusResponse(
        long retainedMebibytes,
        long retainedBytes,
        int chunkCount,
        long heapUsedBytes,
        long heapCommittedBytes,
        long heapMaxBytes,
        double heapUsagePercent
) {
}
