package com.sentinelai.sentinel.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiEnrichment(
        String summary,
        @JsonProperty("likely_cause") String likelyCause,
        @JsonProperty("severity_assessment") String severityAssessment
) {}
