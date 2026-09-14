package com.sentinelai.sentinel.classifier.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class OrderStateAnomalyRule extends AbstractClassificationRule {

    private static final String RULE_ID = "order.state-anomaly.v1";
    private static final Pattern ORDER_STATE_PATTERN = Pattern.compile("(?i)\\border state\\b");
    private static final String KEY_ORDER_ID = "orderId";
    private static final String KEY_FROM = "from";
    private static final String KEY_TO = "to";

    public OrderStateAnomalyRule() {
        super(RULE_ID);
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if(!matchesPatterns(signal, ORDER_STATE_PATTERN)) return  Optional.empty();

        String orderId = hintAsString(signal, KEY_ORDER_ID);
        if (orderId == null) {
            return Optional.empty();
        }

        String from = hintAsString(signal, KEY_FROM);
        String to = hintAsString(signal, KEY_TO);

        Map<String, Object> payload = new HashMap<>();
        payload.put(KEY_ORDER_ID, orderId);
        if (from != null) payload.put(KEY_FROM, from);
        if (to != null) payload.put(KEY_TO, to);

        return Optional.of(OperationalEvent.fromRule(
                signal,
                RULE_ID,
                FailureType.ORDER_STATE_ANOMALY,
                Severity.ERROR,
                payload)
        );
    }
}
