package com.practice.observability_lab.mockexternal;

import com.practice.observability_lab.mockexternal.dto.MockExternalResponse;
import com.practice.observability_lab.mockexternal.exception.InvalidMockExternalRequestException;
import com.practice.observability_lab.mockexternal.exception.MockExternalErrorException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Profile("external-api")
@Service
public class MockExternalService {

    private static final int MIN_ERROR_STATUS = 400;
    private static final int MAX_ERROR_STATUS = 599;

    private final MockExternalProperties properties;
    private final AtomicInteger inflightRequests = new AtomicInteger();
    private final Counter normalSuccessCounter;
    private final Counter delaySuccessCounter;
    private final Counter errorCounter;
    private final Timer normalDurationTimer;
    private final Timer delayDurationTimer;
    private final Timer errorDurationTimer;

    public MockExternalService(
            MockExternalProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        validateProperties();

        Gauge.builder("lab.mock.external.inflight", inflightRequests, AtomicInteger::get)
                .description("Mock external API requests currently being processed")
                .register(meterRegistry);

        normalSuccessCounter = requestCounter(meterRegistry, "normal", "success");
        delaySuccessCounter = requestCounter(meterRegistry, "delay", "success");
        errorCounter = requestCounter(meterRegistry, "error", "error");
        normalDurationTimer = durationTimer(meterRegistry, "normal");
        delayDurationTimer = durationTimer(meterRegistry, "delay");
        errorDurationTimer = durationTimer(meterRegistry, "error");
    }

    public MockExternalResponse normal() {
        inflightRequests.incrementAndGet();
        long startedAt = System.nanoTime();
        try {
            MockExternalResponse response = response(
                    "normal",
                    "mock external API responded normally",
                    0
            );
            normalSuccessCounter.increment();
            return response;
        } finally {
            recordCompletion(normalDurationTimer, startedAt);
        }
    }

    public MockExternalResponse delay(long milliseconds) throws InterruptedException {
        validateDelay(milliseconds);
        inflightRequests.incrementAndGet();
        long startedAt = System.nanoTime();
        try {
            Thread.sleep(milliseconds);
            MockExternalResponse response = response(
                    "delay",
                    "mock external API responded after a delay",
                    milliseconds
            );
            delaySuccessCounter.increment();
            return response;
        } finally {
            recordCompletion(delayDurationTimer, startedAt);
        }
    }

    public void error(int status) {
        if (status < MIN_ERROR_STATUS || status > MAX_ERROR_STATUS) {
            throw new InvalidMockExternalRequestException(
                    "status must be between " + MIN_ERROR_STATUS + " and " + MAX_ERROR_STATUS
            );
        }

        inflightRequests.incrementAndGet();
        long startedAt = System.nanoTime();
        try {
            errorCounter.increment();
            throw new MockExternalErrorException(status);
        } finally {
            recordCompletion(errorDurationTimer, startedAt);
        }
    }

    private Counter requestCounter(
            MeterRegistry meterRegistry,
            String scenario,
            String outcome
    ) {
        return Counter.builder("lab.mock.external.requests")
                .tag("scenario", scenario)
                .tag("outcome", outcome)
                .description("Mock external API request count")
                .register(meterRegistry);
    }

    private Timer durationTimer(MeterRegistry meterRegistry, String scenario) {
        return Timer.builder("lab.mock.external.duration")
                .tag("scenario", scenario)
                .description("Mock external API processing duration")
                .register(meterRegistry);
    }

    private void recordCompletion(Timer timer, long startedAt) {
        timer.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        inflightRequests.decrementAndGet();
    }

    private MockExternalResponse response(
            String scenario,
            String message,
            long delayedMilliseconds
    ) {
        return new MockExternalResponse(
                scenario,
                message,
                delayedMilliseconds,
                Thread.currentThread().getName(),
                Instant.now()
        );
    }

    private void validateDelay(long milliseconds) {
        if (milliseconds < 0 || milliseconds > properties.getMaxDelayMilliseconds()) {
            throw new InvalidMockExternalRequestException(
                    "milliseconds must be between 0 and "
                            + properties.getMaxDelayMilliseconds()
            );
        }
    }

    private void validateProperties() {
        if (properties.getMaxDelayMilliseconds() < 1) {
            throw new IllegalStateException(
                    "lab.mock-external.max-delay-milliseconds must be at least 1"
            );
        }
    }
}
