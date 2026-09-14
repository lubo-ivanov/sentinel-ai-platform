package com.sentinelai.sentinel.classifier.rules;

import com.sentinelai.sentinel.classifier.ClassificationRule;
import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.micrometer.common.util.StringUtils.isEmpty;

@Component
public class PaymentProviderTimeoutRule implements ClassificationRule {
    private static final String RULE_ID = "payment.provider-timeout.v1";
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(?i)\\btimeout\\b");

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if (isEmpty(signal.getMessage())
                || !TIMEOUT_PATTERN.matcher(signal.getMessage()).find()) {
            return Optional.empty();
        }

        String provider = hint(signal, "provider");
        if (provider == null) {
            return Optional.empty();
        }

        return Optional.of(OperationalEvent.fromRule(
                signal,
                RULE_ID,
                FailureType.PAYMENT_PROVIDER_TIMEOUT,
                Severity.ERROR,
                Map.of("provider", provider)
        ));
    }

    private static String hint(RawSignalEntity signal, String key) {
        Map<String, Object> hints = signal.getHints();
        if (hints == null) return null;
        return Objects.toString(hints.get(key), null);
    }
}
