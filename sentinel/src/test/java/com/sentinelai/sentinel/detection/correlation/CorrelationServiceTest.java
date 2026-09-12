package com.sentinelai.sentinel.detection.correlation;

import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.AnomalyFingerprint;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import com.sentinelai.sentinel.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationServiceTest {

    private static final String RULE_ID = "payment_provider_timeout_burst";
    private static final Anomaly STRIPE_ANOMALY = new Anomaly(
            RULE_ID, Instant.now(), Map.of("provider", "stripe"), 5L, Severity.ERROR
    );

    @Mock
    private IncidentRepository repository;

    private CorrelationService correlationService;

    @BeforeEach
    void setUp() {
        correlationService = new CorrelationService(repository);
    }

    @Test
    void newAnomaly_createsIncident() {
        String fingerprint = AnomalyFingerprint.of(STRIPE_ANOMALY).value();
        when(repository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());

        correlationService.onAnomaly(STRIPE_ANOMALY);

        ArgumentCaptor<IncidentEntity> captor = ArgumentCaptor.forClass(IncidentEntity.class);
        verify(repository).save(captor.capture());
        IncidentEntity saved = captor.getValue();
        assertThat(saved.getFingerprint()).isEqualTo(fingerprint);
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(saved.getAnomalyCount()).isEqualTo(1);
        assertThat(saved.getSeverity()).isEqualTo("LOW");
    }

    @Test
    void repeatAnomaly_updatesExistingIncident() {
        String fingerprint = AnomalyFingerprint.of(STRIPE_ANOMALY).value();
        IncidentEntity existing = new IncidentEntity(
                UUID.randomUUID(), "Anomaly: " + RULE_ID, "LOW", IncidentStatus.OPEN, fingerprint, 3
        );
        when(repository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existing));

        correlationService.onAnomaly(STRIPE_ANOMALY);

        assertThat(existing.getAnomalyCount()).isEqualTo(4);
        assertThat(existing.getLastSeen()).isNotNull();
        verify(repository, never()).save(any());
    }

    @Test
    void differentFingerprints_createSeparateIncidents() {
        Anomaly paypalAnomaly = new Anomaly(
                RULE_ID, Instant.now(), Map.of("provider", "paypal"), 5L, Severity.ERROR
        );
        String stripeFingerprint = AnomalyFingerprint.of(STRIPE_ANOMALY).value();
        String paypalFingerprint = AnomalyFingerprint.of(paypalAnomaly).value();

        assertThat(stripeFingerprint).isNotEqualTo(paypalFingerprint);

        when(repository.findByFingerprintAndStatus(eq(stripeFingerprint), eq(IncidentStatus.OPEN)))
                .thenReturn(Optional.empty());
        when(repository.findByFingerprintAndStatus(eq(paypalFingerprint), eq(IncidentStatus.OPEN)))
                .thenReturn(Optional.empty());

        correlationService.onAnomaly(STRIPE_ANOMALY);
        correlationService.onAnomaly(paypalAnomaly);

        verify(repository).save(argThat(e -> e.getFingerprint().equals(stripeFingerprint)));
        verify(repository).save(argThat(e -> e.getFingerprint().equals(paypalFingerprint)));
    }

    @Test
    void severityLow_whenCountBelow5() {
        String fingerprint = AnomalyFingerprint.of(STRIPE_ANOMALY).value();
        IncidentEntity existing = new IncidentEntity(
                UUID.randomUUID(), "Anomaly: " + RULE_ID, "LOW", IncidentStatus.OPEN, fingerprint, 3
        );
        when(repository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existing));

        correlationService.onAnomaly(STRIPE_ANOMALY);

        assertThat(existing.getSeverity()).isEqualTo("LOW");
    }

    @Test
    void severityMedium_whenCountReaches5() {
        String fingerprint = AnomalyFingerprint.of(STRIPE_ANOMALY).value();
        IncidentEntity existing = new IncidentEntity(
                UUID.randomUUID(), "Anomaly: " + RULE_ID, "LOW", IncidentStatus.OPEN, fingerprint, 4
        );
        when(repository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existing));

        correlationService.onAnomaly(STRIPE_ANOMALY);

        assertThat(existing.getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    void severityHigh_whenCountReaches10() {
        String fingerprint = AnomalyFingerprint.of(STRIPE_ANOMALY).value();
        IncidentEntity existing = new IncidentEntity(
                UUID.randomUUID(), "Anomaly: " + RULE_ID, "MEDIUM", IncidentStatus.OPEN, fingerprint, 9
        );
        when(repository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existing));

        correlationService.onAnomaly(STRIPE_ANOMALY);

        assertThat(existing.getSeverity()).isEqualTo("HIGH");
    }
}