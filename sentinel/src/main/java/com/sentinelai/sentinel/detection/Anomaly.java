package com.sentinelai.sentinel.detection;

import com.sentinelai.sentinel.classifier.Severity;

import java.time.Instant;
import java.util.Map;

public record Anomaly(
        String ruleId,
        Instant firedAt,
        Map<String, Object> keys,
        long count,
        Severity severity
) {
}
