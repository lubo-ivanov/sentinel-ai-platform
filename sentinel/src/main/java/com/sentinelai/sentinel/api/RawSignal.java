package com.sentinelai.sentinel.api;

import java.time.Instant;
import java.util.Map;

public record RawSignal(
        String id,
        String source,
        Instant occurredAt,
        String message,
        Map<String, Object> hints
) {}
