package com.practice.observability_lab.memory;

import com.practice.observability_lab.memory.dto.MemoryAllocationResponse;
import com.practice.observability_lab.memory.dto.MemoryReleaseResponse;
import com.practice.observability_lab.memory.exception.InvalidMemoryAllocationException;
import com.practice.observability_lab.memory.exception.MemoryLimitExceededException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryServiceTests {

    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        MemoryProperties properties = new MemoryProperties();
        properties.setChunkSizeMib(1);
        properties.setMaxAllocationPerRequestMib(2);
        properties.setMaxRetainedMib(3);

        memoryService = new MemoryService(
                properties,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void allocatesAndReleasesRetainedMemory() {
        MemoryAllocationResponse allocation = memoryService.allocate(2);

        assertThat(allocation.allocatedBytes()).isEqualTo(2L * 1024 * 1024);
        assertThat(allocation.totalRetainedMebibytes()).isEqualTo(2);
        assertThat(allocation.chunkCount()).isEqualTo(2);

        MemoryReleaseResponse release = memoryService.release();

        assertThat(release.releasedMebibytes()).isEqualTo(2);
        assertThat(release.releasedChunkCount()).isEqualTo(2);
        assertThat(memoryService.status().retainedBytes()).isZero();
    }

    @Test
    void rejectsAllocationThatExceedsRequestLimit() {
        assertThatThrownBy(() -> memoryService.allocate(3))
                .isInstanceOf(InvalidMemoryAllocationException.class);
    }

    @Test
    void rejectsAllocationThatExceedsTotalLimit() {
        memoryService.allocate(2);

        assertThatThrownBy(() -> memoryService.allocate(2))
                .isInstanceOf(MemoryLimitExceededException.class);
    }
}
