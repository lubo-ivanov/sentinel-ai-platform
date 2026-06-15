package com.sentinelai.sentinel.service;

import com.sentinelai.sentinel.api.Incident;
import com.sentinelai.sentinel.api.IncidentRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IncidentStore {
    private final Map<String, Incident> incidents = new ConcurrentHashMap<>(Map.of(
            "1", new Incident("1", "incident_1", "minor", Instant.now(), "ongoing"),
            "2", new Incident("2", "incident_2", "severe", Instant.now(), "ongoing"),
            "3", new Incident("3", "incident_3", "critical", Instant.now(), "ongoing")
    ));

    public List<Incident> findAll() {
        return List.copyOf(incidents.values());
    }

    public Optional<Incident> findById(String id) {
        return Optional.ofNullable(incidents.get(id));
    }

    public Incident create(IncidentRequest request) {
        Incident incident = new Incident(
                UUID.randomUUID().toString(),
                request.title(),
                request.severity(),
                Instant.now(),
                "OPEN"
        );
        incidents.put(incident.id(), incident);
        return incident;
    }
}
