package com.sentinelai.sentinel.detection;

import com.sentinelai.sentinel.classifier.OperationalEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AnomalyDetectionService {

    private static final String METRIC_FIRED = "anomaly.fired";
    private static final String METRIC_DURATION = "anomaly.detection.duration";
    private static final String TAG_RULE_ID = "ruleId";

    private final List<AnomalyRule> rules;
    private final List<AnomalyListener> listeners;
    private final MeterRegistry meterRegistry;

    public AnomalyDetectionService(List<AnomalyRule> rules,
                                   List<AnomalyListener> listeners,
                                   MeterRegistry meterRegistry) {
        this.rules = rules;
        this.listeners = listeners;
        this.meterRegistry = meterRegistry;
    }

    public void evaluate(OperationalEvent event) {
        for (AnomalyRule rule : rules) {
            Timer timer = Timer.builder(METRIC_DURATION)
                    .tag(TAG_RULE_ID, rule.id())
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);
            try {
                timer.record(() -> rule.evaluate(event).ifPresent(this::dispatch));
            } catch (Exception e) {
                log.error("Rule {} failed evaluating event {}", rule.id(), event.id(), e);
            }
        }
    }

    private void dispatch(Anomaly anomaly) {
        log.info("Anomaly fired: ruleId={}, keys={}, count={}",
                anomaly.ruleId(), anomaly.keys(), anomaly.count());
        meterRegistry.counter(METRIC_FIRED, TAG_RULE_ID, anomaly.ruleId()).increment();
        for (AnomalyListener listener : listeners) {
            try {
                listener.onAnomaly(anomaly);
            } catch (Exception e) {
                log.error("Listener {} failed handling anomaly {}",
                        listener.getClass().getSimpleName(), anomaly.ruleId(), e
                );
            }
        }
    }

}
