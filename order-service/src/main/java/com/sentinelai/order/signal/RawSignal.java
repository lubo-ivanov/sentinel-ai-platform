package com.sentinelai.order.signal;

import java.util.Map;

public record RawSignal(
        String id,
        String source,
        String occurredAt,
        String message,
        Map<String, Object> hints
) {}
