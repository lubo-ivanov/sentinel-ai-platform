package com.sentinelai.sentinel;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class IncidentStore {
    private final List<Incident> incidents = new ArrayList<>(List.of(
            new Incident("1", "incident_1", "minor", Instant.now(), "ongoing"),
            new Incident("2", "incident_2", "severe", Instant.now(), "ongoing"),
            new Incident("3", "incident_3", "critical", Instant.now(), "ongoing")
    ));

    List<Incident> findAll() {
        return incidents;
    }

    Optional<Incident> findById(String id) {
        return incidents.stream().filter(it -> it.id().equals(id)).findFirst();
    }

    Incident create(IncidentRequest request) {
        Incident incident = new Incident(
                UUID.randomUUID().toString(),
                request.title(),
                request.severity(),
                Instant.now(),
                "OPEN"
        );
        incidents.add(incident);
        return incident;
    }
}
