package com.sentinelai.sentinel.classifier.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.RawSignalEntity;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class CheckoutFlowDegradationRule extends AbstractClassificationRule {

    private static final String RULE_ID = "order.checkout-degradation.v1";
    private static final Pattern CHECKOUT_PATTERN = Pattern.compile("(?i)\\bcheckout\\b");
    private static final String KEY_SERVICE = "service";
    private static final String KEY_DURATION = "duration_ms";

    public CheckoutFlowDegradationRule() {
        super(RULE_ID);
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if (!matchesPatterns(signal, CHECKOUT_PATTERN)) return  Optional.empty();

        String service = hintAsString(signal, KEY_SERVICE);
        if (service == null) {
            return Optional.empty();
        }
        String durationMs = hintAsString(signal, KEY_DURATION);
        return Optional.of(OperationalEvent.fromRule(
                        signal,
                        RULE_ID,
                        FailureType.CHECKOUT_FLOW_DEGRADATION,
                        Severity.WARN,
                        durationMs != null
                                ? Map.of(KEY_SERVICE, service, KEY_DURATION, durationMs)
                                : Map.of(KEY_SERVICE, service)
                )
        );
    }
}
