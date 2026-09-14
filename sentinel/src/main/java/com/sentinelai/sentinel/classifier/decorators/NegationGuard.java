package com.sentinelai.sentinel.classifier.decorators;

import com.sentinelai.sentinel.classifier.ClassificationRule;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class NegationGuard implements ClassificationRule {

    private static final Pattern NEGATION_PATTERN = Pattern.compile("(?i)\\b(no|not|without|never)\\b");

    private final ClassificationRule delegate;

    @Override
    public String ruleId() {
        return delegate.ruleId();
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if (signal.getMessage() != null
                && NEGATION_PATTERN.matcher(signal.getMessage()).find()) {
            return Optional.empty();
        }
        return delegate.apply(signal);
    }
}
