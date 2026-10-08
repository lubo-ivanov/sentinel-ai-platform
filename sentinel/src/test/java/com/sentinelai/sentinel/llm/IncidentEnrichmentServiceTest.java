package com.sentinelai.sentinel.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.domain.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentEnrichmentServiceTest {

    @Mock
    private LlmClient llmClient;

    private IncidentEnrichmentService enrichmentService;

    private String capturedResponse;

    @BeforeEach
    void setUp() throws IOException {
        enrichmentService = new IncidentEnrichmentService(llmClient, new ObjectMapper());
        capturedResponse = new String(
                getClass().getClassLoader().getResourceAsStream("ollama-response.json").readAllBytes()
        );
    }

    @Test
    void enrich_parsesValidResponse() {
        IncidentEntity incident = new IncidentEntity(
                UUID.randomUUID(), "Anomaly: payment.provider_timeout.v1",
                "HIGH", IncidentStatus.OPEN, "abc123", 1
        );
        Anomaly anomaly = new Anomaly(
                "payment.provider_timeout.v1",
                Instant.now(),
                Map.of("provider", "stripe"),
                5L,
                Severity.ERROR,
                List.of()
        );
        when(llmClient.generate(anyString())).thenReturn(capturedResponse);

        Optional<AiEnrichment> result = enrichmentService.enrich(incident, anomaly);

        assertThat(result).isPresent();
        assertThat(result.get().summary()).contains("Stripe");
        assertThat(result.get().likelyCause()).isNotBlank();
        assertThat(result.get().severityAssessment()).isNotBlank();
    }

    @Test
    void enrich_returnsEmptyOnOllamaFailure() {
        IncidentEntity incident = new IncidentEntity(
                UUID.randomUUID(), "Anomaly: payment.provider_timeout.v1",
                "HIGH", IncidentStatus.OPEN, "abc123", 1
        );
        Anomaly anomaly = new Anomaly(
                "payment.provider_timeout.v1",
                Instant.now(),
                Map.of("provider", "stripe"),
                5L,
                Severity.ERROR,
                List.of()
        );
        when(llmClient.generate(anyString())).thenThrow(new RuntimeException("Ollama down"));

        Optional<AiEnrichment> result = enrichmentService.enrich(incident, anomaly);

        assertThat(result).isEmpty();
    }
}
