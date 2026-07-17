package com.sentinelai.sentinel.api;

import java.time.Instant;
import java.util.UUID;

public record Incident(
        UUID id,
        String title,
        String severity,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {}
