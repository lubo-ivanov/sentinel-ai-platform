package com.sentinelai.sentinel.service;

import com.sentinelai.sentinel.api.Incident;
import com.sentinelai.sentinel.api.IncidentRequest;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository repository;

    @Transactional(readOnly = true)
    public List<Incident> findAll() {
        return repository.findAll().stream().map(IncidentService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID id) {
        return repository.findById(id).map(IncidentService::toDto);
    }

    public Incident create(IncidentRequest request) {
        IncidentEntity entity = new IncidentEntity(
                UUID.randomUUID(),
                request.title(),
                request.severity(),
                "OPEN"
        );
        IncidentEntity saved = repository.save(entity);
        return toDto(saved);
    }

    private static Incident toDto(IncidentEntity e) {
        return new Incident(
                e.getId(),
                e.getTitle(),
                e.getSeverity(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion()
        );
    }


}
