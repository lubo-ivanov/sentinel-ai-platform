package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.domain.RawSignalEntity;
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

    public RuleEngine(List<ClassificationRule> rules) {
        this.rules = rules;
        log.info("RulesEngine initialised with {} rules: {}",
                rules.size(),
                rules.stream().map((ClassificationRule::ruleId)).toList()
        );
    }

    public OperationalEvent classify(RawSignalEntity signal) {
        for (ClassificationRule rule : rules) {
            Optional<OperationalEvent> match = rule.apply(signal);
            if (match.isPresent()) {
                return match.get();
            }
        }
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
