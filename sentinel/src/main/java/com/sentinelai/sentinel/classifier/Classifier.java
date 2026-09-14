package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.domain.RawSignalEntity;

public interface Classifier {
    OperationalEvent classify(RawSignalEntity signal);
}
