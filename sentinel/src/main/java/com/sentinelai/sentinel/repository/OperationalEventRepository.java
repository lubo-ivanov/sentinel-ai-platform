package com.sentinelai.sentinel.repository;

import com.sentinelai.sentinel.domain.OperationalEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OperationalEventRepository extends JpaRepository<OperationalEventEntity, UUID> {
}
