package com.sentinelai.sentinel.api;

import com.sentinelai.sentinel.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

    @GetMapping
    public List<Incident> findAll() {
        return incidentService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> findById(@PathVariable UUID id) {
        Optional<Incident> incident = incidentService.findById(id);
        return incident.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Incident> create(@RequestBody IncidentRequest request) {
        Incident created = incidentService.create(request);
        return ResponseEntity.status(CREATED).body(created);
    }
}
