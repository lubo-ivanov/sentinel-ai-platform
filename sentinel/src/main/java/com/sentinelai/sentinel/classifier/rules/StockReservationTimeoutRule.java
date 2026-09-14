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
public class StockReservationTimeoutRule extends AbstractClassificationRule {

    private static final String RULE_ID = "inventory.stock-reservation-timeout.v1";
    private static final Pattern PATTERN = Pattern.compile("(?i)\\bstock\\s+reservation\\s+timeout\\b");
    private static final String KEY_ITEM = "item";
    private static final String KEY_WAREHOUSE = "warehouse";

    public StockReservationTimeoutRule() {
        super(RULE_ID);
    }
    @Override
    public Optional<OperationalEvent> apply(RawSignalEntity signal) {
        if(!matchesPatterns(signal, PATTERN)) return  Optional.empty();

        String item = hintAsString(signal, KEY_ITEM);
        String warehouse = hintAsString(signal, KEY_WAREHOUSE);

        if (item == null || warehouse == null) {
            return Optional.empty();
        }

        return Optional.of(OperationalEvent.fromRule(
                signal,
                RULE_ID,
                FailureType.STOCK_RESERVATION_TIMEOUT,
                Severity.ERROR,
                Map.of(KEY_ITEM, item, KEY_WAREHOUSE, warehouse))
        );
    }
}
