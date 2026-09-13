package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.AnomalyRule;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractBurstRule implements AnomalyRule {

    private final SlidingWindowCounter counter;
    private final String ruleId;
    private final FailureType type;
    private final String payloadKey;
    private final Severity severity;

    protected static final Duration WINDOW = Duration.ofSeconds(60);
    protected static final long THRESHOLD = 5;

    @Override
    public String id() {
        return ruleId;
    }

    @Override
    public Optional<Anomaly> evaluate(OperationalEvent event) {
        if (event.type() != type) {
            return Optional.empty();
        }

        Object value = event.payload() == null ? null : event.payload().get(payloadKey);
        if (value == null) {
            return Optional.empty();
        }

        String counterKey = id() + ":" + value;
        Instant now = Instant.now();
        counter.record(counterKey, now, WINDOW);

        long count = counter.count(counterKey, now, WINDOW);
        if (count < THRESHOLD) {
            return Optional.empty();
        }

        return Optional.of(new Anomaly(id(),
                now,
                Map.of(payloadKey, value.toString()),
                count,
                severity)
        );
    }
}
