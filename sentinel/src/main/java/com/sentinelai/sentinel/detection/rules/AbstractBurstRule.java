package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.AnomalyRule;
import com.sentinelai.sentinel.detection.DetectionProperties;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractBurstRule implements AnomalyRule {

    private final SlidingWindowCounter counter;
    private final String ruleId;
    private final FailureType type;
    private final String payloadKey;
    private final Severity severity;
    private final List<String> recentMessages = new ArrayList<>();
    private final DetectionProperties props;


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
        counter.record(counterKey, now, props.window());

        long count = counter.count(counterKey, now, props.window());
        if (count < props.threshold()) return Optional.empty();
        if (!recentMessages.isEmpty() && recentMessages.size() >= props.maxRecentMessages()) recentMessages.removeFirst();
        recentMessages.add(event.message());

        return Optional.of(new Anomaly(id(),
                now,
                Map.of(payloadKey, value.toString()),
                count,
                severity,
                new ArrayList<>(recentMessages)));
    }
}
