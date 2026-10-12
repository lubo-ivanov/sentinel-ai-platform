package com.sentinelai.sentinel.repository;

import com.sentinelai.sentinel.domain.IncidentEnrichmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IncidentEnrichmentRepository extends JpaRepository<IncidentEnrichmentEntity, UUID> {
    Optional<IncidentEnrichmentEntity> findByIncidentId(UUID incidentId);
}
