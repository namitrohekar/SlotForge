package com.slotforge.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("slotforge")
public record SlotForgeProperties(String frontendOrigin, Schema schema) {

    public record Schema(int requiredVersion) {
    }
}
