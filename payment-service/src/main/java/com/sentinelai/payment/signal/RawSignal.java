package com.sentinelai.payment.signal;

import java.util.Map;

public record RawSignal(String id, String occurredAt, String message, Map<String, Object> hints) {}
