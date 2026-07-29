package com.sentinelai.sentinel.detection;

import com.sentinelai.sentinel.classifier.OperationalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final List<AnomalyRule> rules;
    private final List<AnomalyListener> listeners;

    public void evaluate(OperationalEvent event) {
        for (AnomalyRule rule : rules) {
            try {
                rule.evaluate(event).ifPresent(this::dispatch);
            } catch (Exception e) {
                log.error("Rule {} failed evaluating event {}", rule.id(), event.id(), e);
            }
        }
    }

    private void dispatch(Anomaly anomaly) {
        log.info("Anomaly fired: ruleId={}, keys={}, count={}",
                anomaly.ruleId(), anomaly.keys(), anomaly.count()
        );
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
