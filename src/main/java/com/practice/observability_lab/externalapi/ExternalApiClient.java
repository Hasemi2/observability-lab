package com.practice.observability_lab.externalapi;

import com.practice.observability_lab.mockexternal.dto.MockExternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Profile("external-api")
@RequiredArgsConstructor
@Component
public class ExternalApiClient {

    private final RestClient restClient;

    public MockExternalResponse callNormal() {
        return restClient.get()
                .uri("/mock/external/normal")
                .retrieve()
                .body(MockExternalResponse.class);
    }

    public MockExternalResponse callDelay(long milliseconds) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mock/external/delay")
                        .queryParam("milliseconds", milliseconds)
                        .build())
                .retrieve()
                .body(MockExternalResponse.class);
    }

    public MockExternalResponse callError(int status) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mock/external/error")
                        .queryParam("status", status)
                        .build())
                .retrieve()
                .body(MockExternalResponse.class);
    }

}
