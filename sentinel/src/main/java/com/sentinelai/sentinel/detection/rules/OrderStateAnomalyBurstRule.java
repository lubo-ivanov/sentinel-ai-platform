package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.DetectionProperties;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import org.springframework.stereotype.Component;

@Component
public class OrderStateAnomalyBurstRule extends AbstractBurstRule {

    private static final String RULE_ID = "order_state_anomaly_burst";
    private static final String PAYLOAD_KEY = "orderId";

    public OrderStateAnomalyBurstRule(SlidingWindowCounter counter, DetectionProperties props) {
        super(counter, RULE_ID, FailureType.ORDER_STATE_ANOMALY, PAYLOAD_KEY, Severity.ERROR, props);
    }

}
