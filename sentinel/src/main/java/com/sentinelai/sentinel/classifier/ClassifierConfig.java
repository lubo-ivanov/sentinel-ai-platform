package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.classifier.decorators.AuditRule;
import com.sentinelai.sentinel.classifier.decorators.LoggingRule;
import com.sentinelai.sentinel.classifier.decorators.NegationGuard;
import com.sentinelai.sentinel.classifier.rules.CheckoutFlowDegradationRule;
import com.sentinelai.sentinel.classifier.rules.OrderStateAnomalyRule;
import com.sentinelai.sentinel.classifier.rules.PaymentProviderTimeoutRule;
import com.sentinelai.sentinel.classifier.rules.StockConsistencyRiskRule;
import com.sentinelai.sentinel.classifier.rules.StockReservationTimeoutRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClassifierConfig {

    @Bean
    public ClassificationRule paymentProviderTimeoutRule() {
        return new NegationGuard(new AuditRule( new LoggingRule(new PaymentProviderTimeoutRule())));
    }

    @Bean
    public ClassificationRule checkoutFlowDegradationRule() {
        return new AuditRule( new LoggingRule(new CheckoutFlowDegradationRule()));
    }

    @Bean
    public ClassificationRule orderStateAnomalyRule() {
        return new AuditRule( new LoggingRule(new OrderStateAnomalyRule()));
    }

    @Bean
    public ClassificationRule stockReservationTimeoutRule() {
        return new AuditRule( new LoggingRule(new StockReservationTimeoutRule()));
    }

    @Bean
    public ClassificationRule stockConsistencyRiskRule() {
        return new AuditRule( new LoggingRule(new StockConsistencyRiskRule()));
    }

}
