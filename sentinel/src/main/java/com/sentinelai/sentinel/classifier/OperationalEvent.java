package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.domain.RawSignalEntity;

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
        public enum Method { RULE, LLM, MANUAL, FAILURE }
    }

    public static OperationalEvent fromRule(
            RawSignalEntity signal,
            String ruleId,
            FailureType failureType,
            Severity severity,
            Map<String, Object> payload) {
        return new OperationalEvent(
                UUID.randomUUID(),
                signal.getId(),
                signal.getSource(),
                signal.getOccurredAt(),
                failureType,
                severity,
                new Classification(Classification.Method.RULE, ruleId, 1.0),
                payload
        );
    }
}
