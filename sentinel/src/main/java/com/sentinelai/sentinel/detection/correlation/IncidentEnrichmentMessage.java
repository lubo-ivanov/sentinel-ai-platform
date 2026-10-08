package com.sentinelai.sentinel.detection.correlation;

import com.sentinelai.sentinel.detection.Anomaly;

import java.util.UUID;

public record IncidentEnrichmentMessage(
        UUID incidentId,
        Anomaly anomaly
) {}
