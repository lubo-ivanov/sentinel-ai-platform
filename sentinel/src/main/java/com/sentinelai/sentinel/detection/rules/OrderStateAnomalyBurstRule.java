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
public class OrderStateAnomalyBurstRule implements AnomalyRule {

    private static final String RULE_ID = "order_state_anomaly_burst";
    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final long THRESHOLD = 5;

    private final SlidingWindowCounter counter;

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Anomaly> evaluate(OperationalEvent event) {
        if (event.type() != FailureType.ORDER_STATE_ANOMALY) {
            return Optional.empty();
        }

        Object orderId = event.payload() == null ? null : event.payload().get("orderId");
        if (orderId == null) {
            return  Optional.empty();
        }

        String counterKey = RULE_ID + ":" + orderId;
        Instant now = Instant.now();
        counter.record(counterKey, now, WINDOW);

        long count = counter.count(counterKey, now, WINDOW);
        if (count < THRESHOLD) {
            return  Optional.empty();
        }

        return Optional.of(new Anomaly(
                RULE_ID,
                now,
                Map.of("orderId", orderId.toString()),
                count,
                Severity.ERROR
        ));
    }
}
