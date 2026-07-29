package com.sentinelai.sentinel.detection;

import com.sentinelai.sentinel.classifier.OperationalEvent;

import java.util.Optional;

public interface AnomalyRule {

    String id();

    Optional<Anomaly> evaluate(OperationalEvent event);
}

