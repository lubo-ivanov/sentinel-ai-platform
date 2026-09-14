package com.sentinelai.sentinel.classifier.rules;

import com.sentinelai.sentinel.classifier.ClassificationRule;
import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.micrometer.common.util.StringUtils.isEmpty;

@Component
public class OrderStateAnomalyRule implements ClassificationRule {

    private static final String RULE_ID = "order.state-anomaly.v1";
    private static final Pattern ORDER_STATE_PATTERN = Pattern.compile("(?i)\\border state\\b");

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if(isEmpty(signal.getMessage())
        || !ORDER_STATE_PATTERN.matcher(signal.getMessage()).find()) {
            return  Optional.empty();
        }

        String orderId = hintAsString(signal, "orderId");
        if (orderId == null) {
            return Optional.empty();
        }

        String from = hintAsString(signal, "from");
        String to = hintAsString(signal, "to");

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        if (from != null) payload.put("from", from);
        if (to != null) payload.put("to", to);

        return Optional.of(OperationalEvent.fromRule(
                signal,
                RULE_ID,
                FailureType.ORDER_STATE_ANOMALY,
                Severity.ERROR,
                payload)
        );
    }

    private static String hintAsString(RawSignalEntity signal, String key) {
        Map<String, Object> hints = signal.getHints();
        if (hints == null) {
            return null;
        }
        return Objects.toString(hints.get(key), null);
    }
}
