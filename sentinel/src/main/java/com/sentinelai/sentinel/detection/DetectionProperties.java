package com.sentinelai.sentinel.detection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "sentinel.detection")
public record DetectionProperties(
        Duration window,
        long threshold,
        int maxRecentMessages,
        int reenrichMultiplier
) {}
