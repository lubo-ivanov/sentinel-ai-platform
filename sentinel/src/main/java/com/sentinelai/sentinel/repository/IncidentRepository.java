package com.sentinelai.sentinel.repository;

import com.sentinelai.sentinel.domain.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {
}
