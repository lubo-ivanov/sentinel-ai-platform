package com.sentinelai.sentinel.detection.correlation;

import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.AnomalyFingerprint;
import com.sentinelai.sentinel.detection.DetectionProperties;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentSeverity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import com.sentinelai.sentinel.kafka.IncidentEventPublisher;
import com.sentinelai.sentinel.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CorrelationService implements AnomalyListener {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher eventPublisher;
    private final DetectionProperties props;

    @Override
    public void onAnomaly(Anomaly anomaly) {
        String fingerprint = AnomalyFingerprint.of(anomaly).value();
        Optional<IncidentEntity> existing = incidentRepository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN);
        IncidentEntity incident = existing
                .map(this::updateIncident)
                .orElseGet(() -> createIncident(fingerprint, anomaly));

        boolean shouldEnrich = existing.isEmpty() ||
                incident.getAnomalyCount() % (props.threshold() * props.reenrichMultiplier()) == 0;

        if (shouldEnrich) this.scheduleEnrichment(incident, anomaly);
    }

    private IncidentEntity updateIncident(IncidentEntity incident) {
        incident.setAnomalyCount(incident.getAnomalyCount() + 1);
        incident.setLastSeen(Instant.now());
        incident.setSeverity(resolveSeverity(incident.getAnomalyCount()));
        return incident;
    }

    private IncidentEntity createIncident(String fingerprint, Anomaly anomaly) {
        IncidentEntity entity = new IncidentEntity(
                UUID.randomUUID(),
                "Anomaly: " + anomaly.ruleId(),
                resolveSeverity(1),
                IncidentStatus.OPEN,
                fingerprint,
                1
        );

        return incidentRepository.save(entity);
    }

    private void scheduleEnrichment(IncidentEntity incident, Anomaly anomaly) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publish("incidents.enrichment", incident.getId().toString(),
                        new IncidentEnrichmentMessage(incident.getId(), anomaly));
            }
        });
    }


    private static String resolveSeverity(int count) {
        if (count >= 10) return IncidentSeverity.HIGH.name();
        if (count >= 5) return IncidentSeverity.MEDIUM.name();
        return IncidentSeverity.LOW.name();
    }
}