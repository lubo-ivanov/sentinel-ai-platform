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
public class StockConsistencyRiskRule extends AbstractClassificationRule {

    private static final String RULE_ID = "inventory.stock-consistency-risk.v1";
    private static final Pattern PATTERN = Pattern.compile("(?i)\\bstock\\s+count\\s+mismatch\\b");
    private static final String KEY_ITEM = "item";
    private static final String KEY_EXPECTED = "expected";
    private static final String KEY_ACTUAL = "actual";

    public StockConsistencyRiskRule() {
        super(RULE_ID);
    }

    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if(!matchesPatterns(signal, PATTERN)) return  Optional.empty();

        String item = hintAsString(signal, KEY_ITEM);
        String expected = hintAsString(signal, KEY_EXPECTED);
        String actual = hintAsString(signal, KEY_ACTUAL);
        if (item == null || expected == null || actual == null) {
            return Optional.empty();
        }

        return Optional.of(OperationalEvent.fromRule(
                signal,
                RULE_ID,
                FailureType.STOCK_CONSISTENCY_RISK,
                Severity.WARN,
                Map.of(KEY_ITEM, item, KEY_EXPECTED, expected, KEY_ACTUAL, actual))
        );
    }
}
