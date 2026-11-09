package com.slotforge.common.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** google is null unless the google profile is active. */
@ConfigurationProperties("slotforge")
public record SlotForgeProperties(String frontendOrigin, Schema schema, Google google) {

    public record Schema(int requiredVersion) {
    }

    public record Google(Set<String> allowedDomains) {
    }
}
