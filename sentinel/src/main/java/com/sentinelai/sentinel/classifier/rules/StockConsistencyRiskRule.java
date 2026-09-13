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

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class StockConsistencyRiskRule implements ClassificationRule {

    private static final String RULE_ID = "inventory.stock-consistency-risk.v1";
    private static final Pattern PATTERN = Pattern.compile("(?i)\\bstock\\s+count\\s+mismatch\\b");

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if (isEmpty(signal.getMessage())
        || !PATTERN.matcher(signal.getMessage()).find()) {
            return  Optional.empty();
        }

        String item = hintAsString(signal, "item");
        String expected = hintAsString(signal, "expected");
        String actual = hintAsString(signal, "actual");
        if (item == null || expected == null || actual == null) {
            return  Optional.empty();
        }

        return Optional.of(new OperationalEvent(
                UUID.randomUUID(),
                signal.getId(),
                signal.getSource(),
                signal.getOccurredAt(),
                FailureType.STOCK_CONSISTENCY_RISK,
                Severity.WARN,
                new OperationalEvent.Classification(OperationalEvent.Classification.Method.RULE, RULE_ID, 1.0),
                Map.of("item", item, "expected", expected, "actual", actual))
        );
    }

    private static String hintAsString(RawSignalEntity signal, String key) {
        Map<String, Object> hints = signal.getHints();
        if (hints == null) return null;
        return Objects.toString(hints.get(key), null);
    }
}
