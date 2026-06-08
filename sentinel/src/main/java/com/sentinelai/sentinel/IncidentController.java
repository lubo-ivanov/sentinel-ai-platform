package com.sentinelai.sentinel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {
    private final IncidentStore incidentStore;
    public IncidentController(IncidentStore incidentStore) {
        this.incidentStore = incidentStore;
    }

    @GetMapping
    public List<Incident> findAll() {
        return incidentStore.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> findById(@PathVariable String id) {
        Optional<Incident> incident = incidentStore.findById(id);
        return incident.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
