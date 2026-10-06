package com.practice.observability_lab.dblock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Profile("db-lock")
@Component
@ConfigurationProperties(prefix = "lab.db-lock")
public class DbLockProperties {

    private long maxHoldSeconds;
}

