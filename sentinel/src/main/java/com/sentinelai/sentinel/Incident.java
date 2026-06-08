package com.sentinelai.sentinel;

import java.time.Instant;

public record Incident(String id, String title, String severity, Instant createdAt, String status) {
}
