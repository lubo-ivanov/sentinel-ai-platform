package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.domain.RawSignalEntity;

import java.util.Optional;

public interface ClassificationRule {
    String ruleId();
    Optional<OperationalEvent> apply(RawSignalEntity signal);
}
