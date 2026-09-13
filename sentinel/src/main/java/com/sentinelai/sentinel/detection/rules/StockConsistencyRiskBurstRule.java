package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import org.springframework.stereotype.Component;

@Component
public class StockConsistencyRiskBurstRule extends AbstractBurstRule {

    private static final String RULE_ID = "stock_consistency_risk_burst";
    private static final String PAYLOAD_KEY = "item";

    public StockConsistencyRiskBurstRule(SlidingWindowCounter counter) {
        super(counter, RULE_ID, FailureType.STOCK_CONSISTENCY_RISK, PAYLOAD_KEY, Severity.WARN);
    }

}
