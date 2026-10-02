package com.practice.observability_lab.gc;

import com.practice.observability_lab.gc.dto.GcChurnResponse;
import com.practice.observability_lab.gc.exception.GcChurnAlreadyRunningException;
import com.practice.observability_lab.gc.exception.InvalidGcChurnRequestException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Profile("gc")
@Service
public class GcService {

    private static final long BYTES_PER_MEBIBYTE = 1024L * 1024L;
    private static final long BYTES_PER_KIBIBYTE = 1024L;
    private static final int PAGE_SIZE_BYTES = 4096;

    private final GcProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Counter executionCounter;
    private final Counter allocatedBytesCounter;
    private final Timer executionTimer;

    private volatile byte[] allocationSink;

    public GcService(GcProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        validateProperties();

        this.executionCounter = Counter.builder("lab.gc.churn.executions")
                .description("Number of completed GC churn experiments")
                .register(meterRegistry);
        this.allocatedBytesCounter = Counter.builder("lab.gc.churn.allocated")
                .baseUnit("bytes")
                .description("Bytes allocated by completed GC churn experiments")
                .register(meterRegistry);
        this.executionTimer = Timer.builder("lab.gc.churn.duration")
                .description("Duration of GC churn experiments")
                .register(meterRegistry);
    }

    public GcChurnResponse churn(Long requestedTotalMebibytes, Integer requestedChunkKibibytes) {
        long totalMebibytes = requestedTotalMebibytes == null
                ? properties.getDefaultTotalMib()
                : requestedTotalMebibytes;
        int chunkKibibytes = requestedChunkKibibytes == null
                ? properties.getDefaultChunkKib()
                : requestedChunkKibibytes;

        validateRequest(totalMebibytes, chunkKibibytes);
        if (!running.compareAndSet(false, true)) {
            throw new GcChurnAlreadyRunningException();
        }

        long startedAt = System.nanoTime();
        long allocatedBytes = 0;
        long allocationCount = 0;
        long checksum = 0;

        try {
            long targetBytes = Math.multiplyExact(totalMebibytes, BYTES_PER_MEBIBYTE);
            int chunkBytes = Math.toIntExact(
                    Math.multiplyExact((long) chunkKibibytes, BYTES_PER_KIBIBYTE)
            );

            while (allocatedBytes < targetBytes) {
                int nextSize = Math.toIntExact(Math.min(chunkBytes, targetBytes - allocatedBytes));
                byte[] chunk = new byte[nextSize];

                // 실제 메모리 페이지를 사용하고 JIT가 할당을 제거하지 못하도록 값을 기록한다.
                for (int index = 0; index < nextSize; index += PAGE_SIZE_BYTES) {
                    chunk[index] = (byte) (allocationCount + index);
                    checksum += chunk[index];
                }

                allocationSink = chunk;
                allocatedBytes += nextSize;
                allocationCount++;
            }

            long durationNanos = System.nanoTime() - startedAt;
            executionCounter.increment();
            allocatedBytesCounter.increment(allocatedBytes);
            executionTimer.record(durationNanos, TimeUnit.NANOSECONDS);

            return new GcChurnResponse(
                    totalMebibytes,
                    chunkKibibytes,
                    allocatedBytes,
                    allocationCount,
                    TimeUnit.NANOSECONDS.toMillis(durationNanos),
                    checksum
            );
        } finally {
            allocationSink = null;
            running.set(false);
        }
    }

    private void validateRequest(long totalMebibytes, int chunkKibibytes) {
        if (totalMebibytes < 1 || totalMebibytes > properties.getMaxTotalMib()) {
            throw new InvalidGcChurnRequestException(
                    "totalMebibytes must be between 1 and " + properties.getMaxTotalMib()
            );
        }
        if (chunkKibibytes < 1 || chunkKibibytes > properties.getMaxChunkKib()) {
            throw new InvalidGcChurnRequestException(
                    "chunkKibibytes must be between 1 and " + properties.getMaxChunkKib()
            );
        }
    }

    private void validateProperties() {
        if (properties.getDefaultTotalMib() < 1
                || properties.getDefaultTotalMib() > properties.getMaxTotalMib()) {
            throw new IllegalStateException(
                    "lab.gc.default-total-mib must be between 1 and lab.gc.max-total-mib"
            );
        }
        if (properties.getDefaultChunkKib() < 1
                || properties.getDefaultChunkKib() > properties.getMaxChunkKib()) {
            throw new IllegalStateException(
                    "lab.gc.default-chunk-kib must be between 1 and lab.gc.max-chunk-kib"
            );
        }
        if (properties.getMaxChunkKib() > Integer.MAX_VALUE / BYTES_PER_KIBIBYTE) {
            throw new IllegalStateException("lab.gc.max-chunk-kib is too large");
        }
    }
}
