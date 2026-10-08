package com.sentinelai.sentinel.detection.correlation;

import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.AnomalyFingerprint;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import com.sentinelai.sentinel.llm.AiSummaryStatus;
import com.sentinelai.sentinel.llm.IncidentEnrichmentService;
import com.sentinelai.sentinel.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CorrelationService implements AnomalyListener {

    private final IncidentRepository incidentRepository;
    private final IncidentEnrichmentService enrichmentService;

    @Override
    public void onAnomaly(Anomaly anomaly) {
        String fingerprint = AnomalyFingerprint.of(anomaly).value();
        incidentRepository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN)
                .ifPresentOrElse(
                        CorrelationService::updateIncident,
                        () -> createIncident(fingerprint, anomaly)
                );
    }

    private static void updateIncident(IncidentEntity incident) {
        incident.setAnomalyCount(incident.getAnomalyCount() + 1);
        incident.setLastSeen(Instant.now());
        incident.setSeverity(resolveSeverity(incident.getAnomalyCount()));
    }

    private void createIncident(String fingerprint, Anomaly anomaly) {
        IncidentEntity entity = new IncidentEntity(
                UUID.randomUUID(),
                "Anomaly: " + anomaly.ruleId(),
                resolveSeverity(1),
                IncidentStatus.OPEN,
                fingerprint,
                1
        );

        enrich(entity, anomaly);
        incidentRepository.save(entity);
    }

    private void enrich(IncidentEntity incident, Anomaly anomaly) {
        enrichmentService.enrich(incident, anomaly).ifPresentOrElse(ai -> {
                    incident.setAiSummary(ai.summary());
                    incident.setAiLikelyCause(ai.likelyCause());
                    incident.setAiGeneratedAt(Instant.now());
                    incident.setAiSummaryStatus(AiSummaryStatus.COMPLETED);
                },
                () -> incident.setAiSummaryStatus(AiSummaryStatus.FAILED));
    }

    private static String resolveSeverity(int count) {
        if (count >= 10) return "HIGH";
        if (count >= 5) return "MEDIUM";
        return "LOW";
    }
}