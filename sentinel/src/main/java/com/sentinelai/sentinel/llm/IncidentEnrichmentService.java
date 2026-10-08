package com.sentinelai.sentinel.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.domain.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentEnrichmentService {

    private static final String PROMPT_TEMPLATE = """
        You are an SRE assistant analyzing a production incident.
        Return ONLY valid JSON with keys: summary, likely_cause, severity_assessment.

        Incident details:
        - Rule fired: %s
        - Affected subject: %s
        - Severity: %s
        - Burst count (last window): %d
        - Total anomaly count: %d
        - First fired: %s
        - Incident first seen: %s

        JSON response:
        """;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public Optional<AiEnrichment> enrich(IncidentEntity incident, Anomaly anomaly) {
        try {
            String prompt = buildPrompt(incident, anomaly);
            String raw = ollamaClient.generate(prompt);
            AiEnrichment enrichment = objectMapper.readValue(raw, AiEnrichment.class);
            return Optional.of(enrichment);
        } catch (Exception e) {
            log.error("LLM enrichment failed for incident id={}", incident.getId(), e);
            return Optional.empty();
        }
    }

    private String buildPrompt(IncidentEntity incident, Anomaly anomaly) {
        return PROMPT_TEMPLATE.formatted(
                anomaly.ruleId(),
                anomaly.keys(),
                anomaly.severity(),
                anomaly.count(),
                incident.getAnomalyCount(),
                anomaly.firedAt(),
                incident.getFirstSeen()
        );
    }
}
