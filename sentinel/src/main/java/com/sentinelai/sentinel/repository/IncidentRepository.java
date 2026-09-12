package com.sentinelai.sentinel.repository;

import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {
    public Optional<IncidentEntity> findByFingerprintAndStatus(String fingerprint, IncidentStatus status);
}
