package com.practice.observability_lab.memory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Profile("memory")
@Component
@ConfigurationProperties(prefix = "lab.memory")
public class MemoryProperties {

    private long chunkSizeMib;
    private long maxAllocationPerRequestMib;
    private long maxRetainedMib;
}
