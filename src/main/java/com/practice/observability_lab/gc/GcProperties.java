package com.practice.observability_lab.gc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Profile("gc")
@Component
@ConfigurationProperties(prefix = "lab.gc")
public class GcProperties {

    private long defaultTotalMib;
    private int defaultChunkKib;
    private long maxTotalMib;
    private int maxChunkKib;
}
