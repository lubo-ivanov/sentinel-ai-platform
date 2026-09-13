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
public class StockReservationTimeoutBurstRule implements AnomalyRule {

    private static final String RULE_ID = "stock_reservation_timeout_burst";
    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final long THRESHOLD = 5;

    private final SlidingWindowCounter counter;

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Anomaly> evaluate(OperationalEvent event) {
        if (event.type() != FailureType.STOCK_RESERVATION_TIMEOUT) {
            return Optional.empty();
        }

        Object item = event.payload() == null ? null : event.payload().get("item");
        if (item == null) {
            return  Optional.empty();
        }

        String counterKey = RULE_ID + ":" + item;
        Instant now = Instant.now();
        counter.record(counterKey, now, WINDOW);

        long count = counter.count(counterKey, now, WINDOW);
        if (count < THRESHOLD) {
            return  Optional.empty();
        }

        return Optional.of(new Anomaly(
                RULE_ID,
                now,
                Map.of("item", item.toString()),
                count,
                Severity.ERROR
        ));
    }
}
