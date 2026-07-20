package com.sentinelai.sentinel.classifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OperationalEvent(
        UUID id,
        UUID sourceSignalId,
        String source,
        Instant timestamp,
        FailureType type,
        Severity severity,
        Classification classification,
        Map<String, Object> payload
) {
    public record Classification(
            Method method,
            String ruleId,
            double confidence
    ) {
        public enum Method { RULE, LLM, MANUAL }
    }
}
