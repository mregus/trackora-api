package com.fleetwise.api.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model
) {}