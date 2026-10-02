package com.practice.observability_lab.gc;

import com.practice.observability_lab.gc.dto.GcChurnResponse;
import com.practice.observability_lab.gc.exception.InvalidGcChurnRequestException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GcServiceTests {

    private SimpleMeterRegistry meterRegistry;
    private GcService gcService;

    @BeforeEach
    void setUp() {
        GcProperties properties = new GcProperties();
        properties.setDefaultTotalMib(2);
        properties.setDefaultChunkKib(256);
        properties.setMaxTotalMib(10);
        properties.setMaxChunkKib(1024);

        meterRegistry = new SimpleMeterRegistry();
        gcService = new GcService(properties, meterRegistry);
    }

    @Test
    void churnAllocatesRequestedBytesInTemporaryChunks() {
        GcChurnResponse response = gcService.churn(2L, 256);

        assertThat(response.allocatedBytes()).isEqualTo(2L * 1024 * 1024);
        assertThat(response.allocationCount()).isEqualTo(8);
        assertThat(meterRegistry.counter("lab.gc.churn.executions").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("lab.gc.churn.allocated").count())
                .isEqualTo(2L * 1024 * 1024);
    }

    @Test
    void churnUsesConfiguredDefaultsWhenParametersAreMissing() {
        GcChurnResponse response = gcService.churn(null, null);

        assertThat(response.requestedTotalMebibytes()).isEqualTo(2);
        assertThat(response.chunkKibibytes()).isEqualTo(256);
    }

    @Test
    void churnRejectsRequestsOverConfiguredLimit() {
        assertThatThrownBy(() -> gcService.churn(11L, 256))
                .isInstanceOf(InvalidGcChurnRequestException.class)
                .hasMessageContaining("between 1 and 10");
    }
}
