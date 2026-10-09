package com.sentinelai.sentinel.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.domain.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
        - Recent signal messages:
            %s

        JSON response:
        """;

    private static final String REMEDIATION_TEMPLATE = """
    You are an SRE assistant. Suggest concrete remediation steps for the following incident.
    Return ONLY a plain text numbered list of actionable steps. No JSON, no preamble.

    Incident summary: %s
    Likely cause: %s
    Severity: %s
    Affected subject: %s
    Recent signal messages:
        %s

    Remediation steps:
    """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties props;

    public Optional<AiEnrichment> enrich(IncidentEntity incident, Anomaly anomaly) {
        for (int attempt = 1; attempt <= props.maxAttempts(); attempt++) {
            try {
                String prompt = buildEnrichmentPrompt(incident, anomaly);
                String raw = llmClient.generate(prompt);
                AiEnrichment enrichment = objectMapper.readValue(raw, AiEnrichment.class);
                return Optional.of(enrichment);
            } catch (Exception e) {
                log.error("LLM enrichment failed for incident id={}", incident.getId(), e);
                sleep(props.retryBackoff());
            }
        }
        return Optional.empty();
    }


    public Optional<String> suggestRemediation(IncidentEntity incident, Anomaly anomaly) {
        for (int attempt = 1; attempt <= props.maxAttempts(); attempt++) {
            try {
                String prompt = buildRemediationPrompt(incident, anomaly);
                return Optional.of(llmClient.generate(prompt));
            } catch (Exception e) {
                log.error("LLM remediation generation failed for incident id={}", incident.getId(), e);
                sleep(props.retryBackoff());
            }
        }
        return Optional.empty();
    }

    private String buildEnrichmentPrompt(IncidentEntity incident, Anomaly anomaly) {
        String description = RULE_DESCRIPTIONS.getOrDefault(anomaly.ruleId(), "Repeated anomaly detected in production");
        String messages = anomaly.recentMessages().stream().filter(Objects::nonNull)
                .map(m -> "- " + m)
                .collect(Collectors.joining("\n"));
        return PROMPT_TEMPLATE.formatted(
                anomaly.ruleId(),
                description,
                anomaly.keys(),
                anomaly.severity(),
                anomaly.count(),
                incident.getAnomalyCount(),
                anomaly.firedAt(),
                incident.getFirstSeen(),
                messages
        );
    }

    private String buildRemediationPrompt(IncidentEntity incident, Anomaly anomaly) {
        String messages = anomaly.recentMessages().stream().filter(Objects::nonNull)
                .map(m -> "- " + m)
                .collect(Collectors.joining("\n"));
        return REMEDIATION_TEMPLATE.formatted(
                Objects.toString(incident.getAiSummary(), "N/A"),
                Objects.toString(incident.getAiLikelyCause(), "N/A"),
                incident.getSeverity(),
                anomaly.keys(),
                messages
        );
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
