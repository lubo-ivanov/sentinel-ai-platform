package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderTimeoutBurstRule extends  AbstractBurstRule {

    private static final String RULE_ID = "payment_provider_timeout_burst";
    private static final String PAYLOAD_KEY = "provider";

    public PaymentProviderTimeoutBurstRule(SlidingWindowCounter counter) {
        super(counter, RULE_ID, FailureType.PAYMENT_PROVIDER_TIMEOUT, PAYLOAD_KEY, Severity.ERROR);
    }

}
