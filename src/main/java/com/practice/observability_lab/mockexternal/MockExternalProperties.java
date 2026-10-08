package com.practice.observability_lab.mockexternal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Profile("external-api")
@Component
@ConfigurationProperties(prefix = "lab.mock-external")
public class MockExternalProperties {

    private long maxDelayMilliseconds;
}

