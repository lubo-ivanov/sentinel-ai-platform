package com.sentinelai.sentinel.service;

import com.sentinelai.sentinel.api.Incident;
import com.sentinelai.sentinel.api.IncidentRequest;
import com.sentinelai.sentinel.domain.IncidentEnrichmentEntity;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import com.sentinelai.sentinel.repository.IncidentEnrichmentRepository;
import com.sentinelai.sentinel.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.sentinelai.sentinel.domain.IncidentStatus.ACKED;
import static com.sentinelai.sentinel.domain.IncidentStatus.OPEN;
import static com.sentinelai.sentinel.domain.IncidentStatus.RESOLVED;
import static java.util.stream.Collectors.toMap;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEnrichmentRepository incidentEnrichmentRepository;

    @Transactional(readOnly = true)
    public List<Incident> findAll() {

        Map<UUID, IncidentEnrichmentEntity> enrichments = incidentEnrichmentRepository.findAll()
                .stream()
                .collect(toMap(IncidentEnrichmentEntity::getIncidentId, e -> e));
        return incidentRepository.findAll()
                .stream()
                .map((e -> toDto(e, enrichments.get(e.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID id) {
        IncidentEnrichmentEntity enrichment = incidentEnrichmentRepository.findByIncidentId(id).orElse(null);
        return incidentRepository.findById(id).map(e -> toDto(e, enrichment));
    }

    public Incident create(IncidentRequest request) {
        IncidentEntity entity = new IncidentEntity(
                UUID.randomUUID(),
                request.title(),
                request.severity(),
                OPEN,
                "",
                1
        );
        IncidentEntity saved = incidentRepository.save(entity);
        return toDto(saved, null);
    }

    public Incident acknowledge(UUID id) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
        if (entity.getStatus() == RESOLVED) {
            throw new IllegalStateException("Cannot acknowledge a resolved incident");
        }

        if (entity.getStatus() == OPEN) {
            entity.setStatus(ACKED);
            entity = incidentRepository.save(entity);
        }

        IncidentEnrichmentEntity enrichment = incidentEnrichmentRepository.findByIncidentId(id).orElse(null);
        return toDto(entity, enrichment);
    }

    public Incident resolve(UUID id) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
        if (entity.getStatus() != RESOLVED) {
            entity.setStatus(IncidentStatus.RESOLVED);
            entity = incidentRepository.save(entity);
        }
        IncidentEnrichmentEntity enrichment = incidentEnrichmentRepository.findByIncidentId(id).orElse(null);
        return toDto(entity, enrichment);
    }


    private static Incident toDto(IncidentEntity e, IncidentEnrichmentEntity ee) {
        boolean hasEnrichment = ee != null;
        return new Incident(
                e.getId(),
                e.getTitle(),
                e.getSeverity(),
                e.getStatus().name(),
                e.getFingerprint(),
                e.getFirstSeen(),
                e.getLastSeen(),
                e.getAnomalyCount(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion(),
                hasEnrichment ? ee.getAiSummary() : null,
                hasEnrichment ? ee.getAiLikelyCause() : null,
                hasEnrichment ? ee.getGeneratedAt() : null,
                hasEnrichment ? ee.getRemediationSteps() : null,
                hasEnrichment ? ee.getAiSummaryStatus() : null
        );
    }


}
