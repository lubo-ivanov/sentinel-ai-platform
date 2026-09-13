package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import org.springframework.stereotype.Component;

@Component
public class StockReservationTimeoutBurstRule extends  AbstractBurstRule {

    private static final String RULE_ID = "stock_reservation_timeout_burst";
    private static final String PAYLOAD_KEY = "item";

    public StockReservationTimeoutBurstRule(SlidingWindowCounter counter) {
        super(counter, RULE_ID, FailureType.STOCK_RESERVATION_TIMEOUT, PAYLOAD_KEY, Severity.ERROR);
    }

}
