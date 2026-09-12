package com.sentinelai.sentinel.service;

import com.sentinelai.sentinel.api.Incident;
import com.sentinelai.sentinel.api.IncidentRequest;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import com.sentinelai.sentinel.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.sentinelai.sentinel.domain.IncidentStatus.ACKED;
import static com.sentinelai.sentinel.domain.IncidentStatus.OPEN;
import static com.sentinelai.sentinel.domain.IncidentStatus.RESOLVED;

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
                OPEN,
                "",
                1
        );
        IncidentEntity saved = repository.save(entity);
        return toDto(saved);
    }

    public Incident acknowledge(UUID id) {
        IncidentEntity entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
        if (entity.getStatus() == RESOLVED) {
            throw new IllegalStateException("Cannot acknowledge a resolved incident");
        }

        if (entity.getStatus() == OPEN) {
            entity.setStatus(ACKED);
            entity = repository.save(entity);
        }
        return toDto(entity);
    }

    public Incident resolve(UUID id) {
        IncidentEntity entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
        if (entity.getStatus() != RESOLVED) {
            entity.setStatus(IncidentStatus.RESOLVED);
            entity = repository.save(entity);
        }
        return toDto(entity);
    }


    private static Incident toDto(IncidentEntity e) {
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
                e.getVersion()
        );
    }


}
