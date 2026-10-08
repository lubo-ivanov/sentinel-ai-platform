package com.sentinelai.sentinel.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.domain.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentEnrichmentService {

    private static final Map<String, String> RULE_DESCRIPTIONS = Map.of(
            "payment_provider_timeout_burst", "External payment gateway (e.g. Stripe) is failing to respond — customers cannot complete purchases",
            "checkout_flow_degradation_burst", "Checkout flow is degraded — orders are failing or slow to process, directly impacting revenue",
            "order_state_anomaly_burst", "Order state machine is producing unexpected transitions — orders may be stuck, duplicated or lost",
            "stock_reservation_timeout_burst", "Stock reservation is timing out — items may be oversold or reservations silently failing",
            "stock_consistency_risk_burst", "Stock levels are inconsistent between services — inventory data may be unreliable, risk of overselling"
    );

    private static final String PROMPT_TEMPLATE = """
        You are an SRE assistant analyzing a production incident.
        Return ONLY valid JSON with keys: summary, likely_cause, severity_assessment.
        Be specific — use the rule description and affected subject in your answer. Do not restate the rule name.

        Incident details:
        - Rule fired: %s
        - What it means: %s
        - Affected subject: %s
        - Severity: %s
        - Burst count (last window): %d
        - Total anomaly count: %d
        - First fired: %s
        - Incident first seen: %s

        JSON response:
        """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public Optional<AiEnrichment> enrich(IncidentEntity incident, Anomaly anomaly) {
        try {
            String prompt = buildPrompt(incident, anomaly);
            String raw = llmClient.generate(prompt);
            AiEnrichment enrichment = objectMapper.readValue(raw, AiEnrichment.class);
            return Optional.of(enrichment);
        } catch (Exception e) {
            log.error("LLM enrichment failed for incident id={}", incident.getId(), e);
            return Optional.empty();
        }
    }

    private String buildPrompt(IncidentEntity incident, Anomaly anomaly) {
        String description = RULE_DESCRIPTIONS.getOrDefault(anomaly.ruleId(), "Repeated anomaly detected in production");
        return PROMPT_TEMPLATE.formatted(
                anomaly.ruleId(),
                description,
                anomaly.keys(),
                anomaly.severity(),
                anomaly.count(),
                incident.getAnomalyCount(),
                anomaly.firedAt(),
                incident.getFirstSeen()
        );
    }
}
