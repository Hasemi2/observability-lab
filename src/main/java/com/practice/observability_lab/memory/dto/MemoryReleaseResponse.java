package com.practice.observability_lab.memory.dto;

public record MemoryReleaseResponse(
        long releasedMebibytes,
        long releasedBytes,
        int releasedChunkCount,
        long remainingRetainedBytes
) {
}
