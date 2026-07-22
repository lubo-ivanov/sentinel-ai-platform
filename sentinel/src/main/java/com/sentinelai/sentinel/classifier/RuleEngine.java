package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.domain.RawSignalEntity;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class RuleEngine {

    private final List<ClassificationRule> rules;
    private final MeterRegistry meterRegistry;
    private final Timer classifyTimer;

    public RuleEngine(List<ClassificationRule> rules, MeterRegistry meterRegistry) {
        this.rules = rules;
        this.meterRegistry = meterRegistry;
        this.classifyTimer = Timer.builder("classifier.duration")
                .description("Time to classify a single signal")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        log.info("RulesEngine initialised with {} rules: {}",
                rules.size(),
                rules.stream().map((ClassificationRule::ruleId)).toList()
        );
    }

    public OperationalEvent classify(RawSignalEntity signal) {
        return classifyTimer.record(() -> doClassify(signal));
    }
    public OperationalEvent doClassify(RawSignalEntity signal) {
        for (ClassificationRule rule : rules) {
            Optional<OperationalEvent> match = rule.apply(signal);
            if (match.isPresent()) {
                meterRegistry.counter("classifier.matched", "ruleId", rule.ruleId()).increment();
                return match.get();
            }
        }

        meterRegistry.counter("classifier.unclassified").increment();
        return unclassified(signal);
    }

    private OperationalEvent unclassified(RawSignalEntity signal) {
        return new OperationalEvent(
                UUID.randomUUID(),
                signal.getId(),
                signal.getSource(),
                signal.getOccurredAt(),
                FailureType.UNCLASSIFIED,
                Severity.INFO,
                new OperationalEvent.Classification(
                        OperationalEvent.Classification.Method.RULE,
                        null,
                        0.0
                ),
                Map.of()
        );
    }
}
