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
public class CheckoutFlowDegradationRule implements ClassificationRule {

    private static final String RULE_ID = "order.checkout-degradation.v1";
    private static final Pattern CHECKOUT_PATTERN = Pattern.compile("(?i)\\bcheckout\\b");

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if (isEmpty(signal.getMessage())
                || !CHECKOUT_PATTERN.matcher(signal.getMessage()).find()) {
            return Optional.empty();
        }

        String service = hint(signal, "service");
        if (service == null) {
            return Optional.empty();
        }
        String durationMs = hint(signal, "duration_ms");
        return Optional.of(OperationalEvent.fromRule(
                        signal,
                        RULE_ID,
                        FailureType.CHECKOUT_FLOW_DEGRADATION,
                        Severity.WARN,
                        durationMs != null
                                ? Map.of("service", service, "duration_ms", durationMs)
                                : Map.of("service", service)
                )
        );
    }

    private static String hint(RawSignalEntity signal, String key) {
        Map<String, Object> hints = signal.getHints();
        if (hints == null) return null;
        return Objects.toString(hints.get(key), null);
    }
}
