package com.sentinelai.sentinel.api;

import com.sentinelai.sentinel.llm.AiSummaryStatus;

import java.time.Instant;
import java.util.UUID;

public record Incident(
        UUID id,
        String title,
        String severity,
        String status,
        String fingerprint,
        Instant firstSeen,
        Instant lastSeen,
        Integer anomalyCount,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        String aiSummary,
        String aiLikelyCause,
        Instant aiGeneratedAt,
        String remediationSteps,
        AiSummaryStatus aiSummaryStatus
) {}
