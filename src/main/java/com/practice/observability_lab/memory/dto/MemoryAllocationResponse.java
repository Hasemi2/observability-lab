package com.practice.observability_lab.memory.dto;

public record MemoryAllocationResponse(
        long requestedMebibytes,
        long allocatedBytes,
        long totalRetainedMebibytes,
        long totalRetainedBytes,
        int chunkCount,
        long maxRetainedMebibytes
) {
}
