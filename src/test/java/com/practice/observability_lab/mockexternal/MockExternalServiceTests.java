package com.practice.observability_lab.mockexternal;

import com.practice.observability_lab.mockexternal.dto.MockExternalResponse;
import com.practice.observability_lab.mockexternal.exception.InvalidMockExternalRequestException;
import com.practice.observability_lab.mockexternal.exception.MockExternalErrorException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockExternalServiceTests {

    private MockExternalService service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        MockExternalProperties properties = new MockExternalProperties();
        properties.setMaxDelayMilliseconds(100);
        meterRegistry = new SimpleMeterRegistry();
        service = new MockExternalService(properties, meterRegistry);
    }

    @Test
    void normalReturnsImmediately() {
        MockExternalResponse response = service.normal();

        assertThat(response.scenario()).isEqualTo("normal");
        assertThat(response.delayedMilliseconds()).isZero();
        assertThat(meterRegistry.counter(
                "lab.mock.external.requests",
                "scenario", "normal",
                "outcome", "success"
        ).count()).isEqualTo(1);
    }

    @Test
    void delayReturnsAfterTheRequestedDelay() throws InterruptedException {
        long startedAt = System.nanoTime();
        MockExternalResponse response = service.delay(20);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(response.scenario()).isEqualTo("delay");
        assertThat(response.delayedMilliseconds()).isEqualTo(20);
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(15);
        assertThat(meterRegistry.timer(
                "lab.mock.external.duration",
                "scenario", "delay"
        ).count()).isEqualTo(1);
        assertThat(meterRegistry.get("lab.mock.external.inflight").gauge().value())
                .isZero();
    }

    @Test
    void delayRejectsValuesOverTheConfiguredLimit() {
        assertThatThrownBy(() -> service.delay(101))
                .isInstanceOf(InvalidMockExternalRequestException.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void errorThrowsTheRequestedHttpStatus() {
        assertThatThrownBy(() -> service.error(503))
                .isInstanceOf(MockExternalErrorException.class)
                .satisfies(exception -> assertThat(
                        ((MockExternalErrorException) exception).getStatus()
                ).isEqualTo(503));
        assertThat(meterRegistry.counter(
                "lab.mock.external.requests",
                "scenario", "error",
                "outcome", "error"
        ).count()).isEqualTo(1);
    }

    @Test
    void errorRejectsNonErrorStatus() {
        assertThatThrownBy(() -> service.error(200))
                .isInstanceOf(InvalidMockExternalRequestException.class)
                .hasMessageContaining("between 400 and 599");
    }
}
