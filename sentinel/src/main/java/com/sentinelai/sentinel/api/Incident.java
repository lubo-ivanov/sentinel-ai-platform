package com.sentinelai.sentinel.api;

import java.time.Instant;

public record Incident(String id, String title, String severity, Instant createdAt, String status) {
}
