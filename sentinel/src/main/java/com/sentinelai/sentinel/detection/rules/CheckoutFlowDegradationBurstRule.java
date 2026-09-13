package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.AnomalyRule;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheckoutFlowDegradationBurstRule implements AnomalyRule {

    private static final String RULE_ID = "checkout_flow_degradation_burst";
    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final long THRESHOLD = 5;

    private final SlidingWindowCounter counter;

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Anomaly> evaluate(OperationalEvent event) {
        if (event.type() != FailureType.CHECKOUT_FLOW_DEGRADATION) {
            return Optional.empty();
        }

        Object service = event.payload() == null ? null : event.payload().get("service");
        if (service == null) {
            return  Optional.empty();
        }

        String counterKey = RULE_ID + ":" + service;
        Instant now = Instant.now();
        counter.record(counterKey, now, WINDOW);

        long count = counter.count(counterKey, now, WINDOW);
        if (count < THRESHOLD) {
            return  Optional.empty();
        }

        return Optional.of(new Anomaly(
                RULE_ID,
                now,
                Map.of("service", service.toString()),
                count,
                Severity.WARN
        ));
    }
}
