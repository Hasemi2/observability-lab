package com.practice.observability_lab.externalapi;

import com.practice.observability_lab.externalapi.dto.ExternalCallResponse;
import com.practice.observability_lab.mockexternal.dto.MockExternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Profile("external-api")
@RequiredArgsConstructor
@Service
public class ExternalApiService {

    private final ExternalApiClient externalApiClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public ExternalCallResponse callNormal() {
        return execute(externalApiClient::callNormal);
    }

    public ExternalCallResponse callDelay(long milliseconds) {
        return execute(() -> externalApiClient.callDelay(milliseconds));
    }

    public ExternalCallResponse callError(int status) {
        return execute(() -> externalApiClient.callError(status));
    }

    private ExternalCallResponse execute(Supplier<MockExternalResponse> externalCall) {
        long startedAt = System.nanoTime();

        return circuitBreakerFactory
                .create("mockExternalApi")
                .run(
                        () -> ExternalCallResponse.success(
                                externalCall.get(),
                                elapsedMilliseconds(startedAt)
                        ),
                        throwable -> ExternalCallResponse.fallback(
                                throwable,
                                elapsedMilliseconds(startedAt)
                        )
                );
    }

    private long elapsedMilliseconds(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }
}
