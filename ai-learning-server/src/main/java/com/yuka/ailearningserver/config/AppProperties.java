package com.yuka.ailearningserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed access to all custom {@code app.*} configuration.
 * Add new settings here instead of scattering {@code @Value} lookups.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors) {

    public record Cors(List<String> allowedOrigins) {
    }
}
