package com.practice.observability_lab.externalapi.config;

import com.practice.observability_lab.externalapi.ExternalApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Profile("external-api")
@Configuration
public class ExternalApiConfig {

    @Bean
    public RestClient mockExternalRestClient(
            RestClient.Builder builder,
            ExternalApiProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                properties.getConnectTimeout()
        );
        requestFactory.setReadTimeout(
                properties.getReadTimeout()
        );

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

}
