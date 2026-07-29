package com.sentinelai.sentinel.detection.rules;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.counter.SlidingWindowCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProviderTimeoutBurstRuleTest {

    private static final String RULE_ID = "payment_provider_timeout_burst";
    private static final String STRIPE = "stripe";
    private static final String COUNTER_KEY_STRIPE = RULE_ID + ":" + STRIPE;

    private SlidingWindowCounter counter;
    private PaymentProviderTimeoutBurstRule rule;

    @BeforeEach
    void setUp() {
        counter = mock(SlidingWindowCounter.class);
        doNothing().when(counter).record(anyString(), any(Instant.class), any(Duration.class));
        rule = new PaymentProviderTimeoutBurstRule(counter);
    }

    @Test
    void doesNotFire_whenEventTypeIsNotTimeout() {
        OperationalEvent event = eventWith(FailureType.PAYMENT_DECLINED, Map.of("provider", STRIPE));

        Optional<Anomaly> result = rule.evaluate(event);

        assertThat(result).isEmpty();
        verify(counter, never()).record(anyString(), any(), any());
    }

    @Test
    void doesNotFire_whenPayloadIsNull() {
        OperationalEvent event = eventWith(FailureType.PAYMENT_PROVIDER_TIMEOUT, null);

        Optional<Anomaly> result = rule.evaluate(event);

        assertThat(result).isEmpty();
        verify(counter, never()).record(anyString(), any(), any());
    }

    @Test
    void doesNotFire_whenProviderIsMissing() {
        OperationalEvent event = eventWith(FailureType.PAYMENT_PROVIDER_TIMEOUT, Map.of("region", "us-east-1"));

        Optional<Anomaly> result = rule.evaluate(event);

        assertThat(result).isEmpty();
        verify(counter, never()).record(anyString(), any(), any());
    }

    @Test
    void doesNotFire_whenCountBelowThreshold() {
        when(counter.count(eq(COUNTER_KEY_STRIPE), any(), any())).thenReturn(4L);

        Optional<Anomaly> result = rule.evaluate(timeoutForStripe());

        assertThat(result).isEmpty();
    }

    @Test
    void fires_whenCountEqualsThreshold() {
        when(counter.count(eq(COUNTER_KEY_STRIPE), any(), any())).thenReturn(5L);

        Optional<Anomaly> result = rule.evaluate(timeoutForStripe());

        assertThat(result).isPresent();
        Anomaly anomaly = result.get();
        assertThat(anomaly.ruleId()).isEqualTo(RULE_ID);
        assertThat(anomaly.keys()).containsEntry("provider", STRIPE);
        assertThat(anomaly.count()).isEqualTo(5L);
        assertThat(anomaly.severity()).isEqualTo(Severity.ERROR);
    }

    @Test
    void fires_whenCountAboveThreshold() {
        when(counter.count(eq(COUNTER_KEY_STRIPE), any(), any())).thenReturn(42L);

        Optional<Anomaly> result = rule.evaluate(timeoutForStripe());

        assertThat(result).isPresent();
        assertThat(result.get().count()).isEqualTo(42L);
    }

    @Test
    void recordsBeforeCounting_soCurrentEventIsIncluded() {
        when(counter.count(eq(COUNTER_KEY_STRIPE), any(), any())).thenReturn(5L);

        rule.evaluate(timeoutForStripe());

        var order = org.mockito.Mockito.inOrder(counter);
        order.verify(counter).record(eq(COUNTER_KEY_STRIPE), any(), any());
        order.verify(counter).count(eq(COUNTER_KEY_STRIPE), any(), any());
    }

    private OperationalEvent timeoutForStripe() {
        return eventWith(FailureType.PAYMENT_PROVIDER_TIMEOUT, Map.of("provider", STRIPE));
    }

    private OperationalEvent eventWith(FailureType type, Map<String, Object> payload) {
        return new OperationalEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "payment-service",
                Instant.now(),
                type,
                Severity.ERROR,
                new OperationalEvent.Classification(OperationalEvent.Classification.Method.RULE, "some-rule", 1.0),
                payload
        );
    }
}
