package com.sentinelai.sentinel.classifier.decorators;

import com.sentinelai.sentinel.classifier.ClassificationRule;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class AuditRule implements ClassificationRule{

    private final ClassificationRule delegate;


    @Override
    public String ruleId() {
        return delegate.ruleId();
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        Optional<OperationalEvent> result = delegate.apply(signal);
        log.info("audit ruleId={} signalId={} source={} matched={}",
                delegate.ruleId(), signal.getId(), signal.getSource(), result.isPresent());
        return result;
    }
}
