package com.sentinelai.sentinel.repository;

import com.sentinelai.sentinel.domain.RawSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RawSignalRepository extends JpaRepository<RawSignalEntity, UUID> {
}
