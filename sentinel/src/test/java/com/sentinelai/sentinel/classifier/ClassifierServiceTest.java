package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.classifier.rules.PaymentProviderTimeoutRule;
import com.sentinelai.sentinel.domain.OperationalEventEntity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import com.sentinelai.sentinel.repository.OperationalEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClassifierServiceTest {

    private static final String SOURCE = "payment-service";
    private static final String EXTERNAL_ID = "ext-1";
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-22T12:00:00Z");
    private static final String TIMEOUT_RULE_ID = "payment.provider-timeout.v1";
    private static final String PROVIDER = "stripe";
    private static final Map<String, Object> PROVIDER_HINT = Map.of("provider", PROVIDER);
    private static final Map<String, Object> NON_PROVIDER_HINT = Map.of("amount", 42.00);
    private static final String TIMEOUT_MESSAGE = "stripe timeout after 5000ms";
    private static final String NON_TIMEOUT_MESSAGE = "payment processed";

    private final OperationalEventRepository repository = mock(OperationalEventRepository.class);
    private final ClassifierService service = new ClassifierService(
            new RuleEngine(List.of(new PaymentProviderTimeoutRule()), new SimpleMeterRegistry()),
            repository
    );

    @Test
    void classifiesTimeoutSignalWithProviderHint() {
        RawSignalEntity signal = signal(TIMEOUT_MESSAGE, PROVIDER_HINT);

        service.classifyAndStore(signal);

        OperationalEventEntity saved = captureSaved();
        assertThat(saved.getType()).isEqualTo(FailureType.PAYMENT_PROVIDER_TIMEOUT);
        assertThat(saved.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(saved.getClassificationRuleId()).isEqualTo(TIMEOUT_RULE_ID);
        assertThat(saved.getPayload()).containsEntry("provider", PROVIDER);
        assertThat(saved.getSourceSignalId()).isEqualTo(signal.getId());
    }

    @Test
    void fallsBackToUnclassifiedWhenProviderMissing() {
        service.classifyAndStore(signal(TIMEOUT_MESSAGE, NON_PROVIDER_HINT));

        assertUnclassified(captureSaved());
    }

    @Test
    void fallsBackToUnclassifiedWhenMessageDoesNotMatch() {
        service.classifyAndStore(signal(NON_TIMEOUT_MESSAGE, PROVIDER_HINT));

        assertUnclassified(captureSaved());
    }

    private RawSignalEntity signal(String message, Map<String, Object> hints) {
        return new RawSignalEntity(
                UUID.randomUUID(),
                EXTERNAL_ID,
                SOURCE,
                OCCURRED_AT,
                message,
                hints
        );
    }

    private OperationalEventEntity captureSaved() {
        ArgumentCaptor<OperationalEventEntity> captor = ArgumentCaptor.forClass(OperationalEventEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private void assertUnclassified(OperationalEventEntity saved) {
        assertThat(saved.getType()).isEqualTo(FailureType.UNCLASSIFIED);
        assertThat(saved.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(saved.getClassificationRuleId()).isNull();
    }
}
