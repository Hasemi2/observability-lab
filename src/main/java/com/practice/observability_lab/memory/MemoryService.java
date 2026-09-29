package com.practice.observability_lab.memory;

import com.practice.observability_lab.memory.dto.MemoryAllocationResponse;
import com.practice.observability_lab.memory.dto.MemoryReleaseResponse;
import com.practice.observability_lab.memory.dto.MemoryStatusResponse;
import com.practice.observability_lab.memory.exception.InvalidMemoryAllocationException;
import com.practice.observability_lab.memory.exception.MemoryLimitExceededException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Profile("memory")
@Service
public class MemoryService {

    private static final long BYTES_PER_MEBIBYTE = 1024L * 1024L;

    private final MemoryProperties properties;
    private final List<byte[]> retainedChunks = new ArrayList<>();
    private final AtomicLong retainedBytesGauge = new AtomicLong();
    private final AtomicInteger retainedChunksGauge = new AtomicInteger();

    public MemoryService(MemoryProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        validateProperties();

        Gauge.builder(
                        "lab.memory.retained",
                        retainedBytesGauge,
                        AtomicLong::get
                )
                .baseUnit("bytes")
                .description("Bytes intentionally retained by the memory scenario")
                .register(meterRegistry);

        Gauge.builder(
                        "lab.memory.chunks",
                        retainedChunksGauge,
                        AtomicInteger::get
                )
                .baseUnit("chunks")
                .description("Chunks intentionally retained by the memory scenario")
                .register(meterRegistry);
    }

    public synchronized MemoryAllocationResponse allocate(long requestedMebibytes) {
        validateRequest(requestedMebibytes);

        long requestedBytes = toBytes(requestedMebibytes);
        long maxRetainedBytes = toBytes(properties.getMaxRetainedMib());
        long retainedAfterAllocation = Math.addExact(
                retainedBytesGauge.get(),
                requestedBytes
        );

        if (retainedAfterAllocation > maxRetainedBytes) {
            throw new MemoryLimitExceededException(
                    retainedBytesGauge.get(),
                    requestedBytes,
                    maxRetainedBytes
            );
        }

        List<byte[]> newChunks = allocateChunks(requestedBytes);
        retainedChunks.addAll(newChunks);
        retainedBytesGauge.set(retainedAfterAllocation);
        retainedChunksGauge.set(retainedChunks.size());

        return new MemoryAllocationResponse(
                requestedMebibytes,
                requestedBytes,
                toMebibytes(retainedAfterAllocation),
                retainedAfterAllocation,
                retainedChunks.size(),
                properties.getMaxRetainedMib()
        );
    }

    public synchronized MemoryStatusResponse status() {
        Runtime runtime = Runtime.getRuntime();
        long heapCommittedBytes = runtime.totalMemory();
        long heapUsedBytes = heapCommittedBytes - runtime.freeMemory();
        long heapMaxBytes = runtime.maxMemory();
        double heapUsagePercent = heapMaxBytes == 0
                ? 0
                : (double) heapUsedBytes / heapMaxBytes * 100;

        return new MemoryStatusResponse(
                toMebibytes(retainedBytesGauge.get()),
                retainedBytesGauge.get(),
                retainedChunks.size(),
                heapUsedBytes,
                heapCommittedBytes,
                heapMaxBytes,
                heapUsagePercent
        );
    }

    public synchronized MemoryReleaseResponse release() {
        long releasedBytes = retainedBytesGauge.get();
        int releasedChunkCount = retainedChunks.size();

        retainedChunks.clear();
        retainedBytesGauge.set(0);
        retainedChunksGauge.set(0);

        return new MemoryReleaseResponse(
                toMebibytes(releasedBytes),
                releasedBytes,
                releasedChunkCount,
                0
        );
    }

    private List<byte[]> allocateChunks(long requestedBytes) {
        long chunkSizeBytes = toBytes(properties.getChunkSizeMib());
        List<byte[]> chunks = new ArrayList<>();
        long remainingBytes = requestedBytes;

        while (remainingBytes > 0) {
            int nextChunkSize = Math.toIntExact(Math.min(chunkSizeBytes, remainingBytes));
            chunks.add(new byte[nextChunkSize]);
            remainingBytes -= nextChunkSize;
        }

        return chunks;
    }

    private void validateRequest(long requestedMebibytes) {
        if (requestedMebibytes < 1) {
            throw new InvalidMemoryAllocationException(
                    "mebibytes must be at least 1"
            );
        }

        if (requestedMebibytes > properties.getMaxAllocationPerRequestMib()) {
            throw new InvalidMemoryAllocationException(
                    "mebibytes must not exceed "
                            + properties.getMaxAllocationPerRequestMib()
            );
        }
    }

    private void validateProperties() {
        if (properties.getChunkSizeMib() < 1) {
            throw new IllegalStateException("lab.memory.chunk-size-mib must be at least 1");
        }

        if (toBytes(properties.getChunkSizeMib()) > Integer.MAX_VALUE) {
            throw new IllegalStateException("lab.memory.chunk-size-mib is too large");
        }

        if (properties.getMaxAllocationPerRequestMib() < 1) {
            throw new IllegalStateException(
                    "lab.memory.max-allocation-per-request-mib must be at least 1"
            );
        }

        if (properties.getMaxRetainedMib()
                < properties.getMaxAllocationPerRequestMib()) {
            throw new IllegalStateException(
                    "lab.memory.max-retained-mib must be greater than or equal to "
                            + "max-allocation-per-request-mib"
            );
        }
    }

    private static long toBytes(long mebibytes) {
        return Math.multiplyExact(mebibytes, BYTES_PER_MEBIBYTE);
    }

    private static long toMebibytes(long bytes) {
        return bytes / BYTES_PER_MEBIBYTE;
    }
}
