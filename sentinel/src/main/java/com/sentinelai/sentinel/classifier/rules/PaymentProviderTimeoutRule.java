package com.sentinelai.sentinel.classifier.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class PaymentProviderTimeoutRule extends AbstractClassificationRule {
    private static final String RULE_ID = "payment.provider-timeout.v1";
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(?i)\\btimeout\\b");
    private static final String KEY_PROVIDER = "provider";

    public PaymentProviderTimeoutRule() {
        super(RULE_ID);
    }


    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if(!matchesPatterns(signal, TIMEOUT_PATTERN)) return  Optional.empty();

        String provider = hintAsString(signal, KEY_PROVIDER);
        if (provider == null) {
            return Optional.empty();
        }

        return Optional.of(OperationalEvent.fromRule(
                signal,
                RULE_ID,
                FailureType.PAYMENT_PROVIDER_TIMEOUT,
                Severity.ERROR,
                Map.of(KEY_PROVIDER, provider)
        ));
    }
}
