package com.sentinelai.sentinel.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "sentinel.llm")
public record LlmProperties(
        String provider,
        int maxAttempts,
        Duration retryBackoff
) {}
