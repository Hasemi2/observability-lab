package com.practice.observability_lab.externalapi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@Profile("external-api")
@ConfigurationProperties(prefix = "lab.external-api")
public class ExternalApiProperties {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
}
