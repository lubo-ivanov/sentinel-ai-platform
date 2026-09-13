package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import org.springframework.stereotype.Component;

@Component
public class CheckoutFlowDegradationBurstRule extends AbstractBurstRule {

    private static final String RULE_ID = "checkout_flow_degradation_burst";
    private static final String PAYLOAD_KEY = "service";

    public CheckoutFlowDegradationBurstRule(SlidingWindowCounter counter) {
        super(counter, RULE_ID, FailureType.CHECKOUT_FLOW_DEGRADATION, PAYLOAD_KEY, Severity.WARN);
    }

}
