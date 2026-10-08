package com.sentinelai.sentinel.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.correlation.IncidentEnrichmentMessage;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.llm.AiEnrichment;
import com.sentinelai.sentinel.llm.AiSummaryStatus;
import com.sentinelai.sentinel.llm.IncidentEnrichmentService;
import com.sentinelai.sentinel.repository.IncidentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class IncidentEnrichmentConsumer extends AbstractKafkaConsumer<IncidentEnrichmentMessage> {

    private static final String TOPIC = "incidents.enrichment";

    private final IncidentRepository incidentRepository;
    private final IncidentEnrichmentService enrichmentService;

    protected IncidentEnrichmentConsumer(
            KafkaConfig kafkaConfig,
            ObjectMapper objectMapper,
            IncidentRepository incidentRepository,
            IncidentEnrichmentService enrichmentService
    ) {
        super(kafkaConfig, objectMapper);
        this.incidentRepository = incidentRepository;
        this.enrichmentService = enrichmentService;
    }

    @Override
    protected String topicName() {
        return TOPIC;
    }

    @Override
    protected Class<IncidentEnrichmentMessage> valueType() {
        return IncidentEnrichmentMessage.class;
    }

    @Override
    protected void process(IncidentEnrichmentMessage message, ConsumerRecord<String, String> raw) {
        incidentRepository.findById(message.incidentId()).ifPresentOrElse(
                incident -> enrichAndSave(incident, message.anomaly()),
                () -> log.warn("Incident not found for enrichment id={}", message.incidentId())
        );
    }

    @Override
    protected void onProcessingFailed(ConsumerRecord<String, String> record, Throwable cause) {
        log.error("Enrichment processing failed for offset={}", record.offset(), cause);
    }

    @Override
    protected void processBatch(List<ConsumerRecord<String, String>> records) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            records.forEach(record -> executor.submit(() -> handleRecord(record)));
        }
    }


    private void enrichAndSave(IncidentEntity incident, Anomaly anomaly) {
        enrichmentService.enrich(incident, anomaly).ifPresentOrElse(
                ai -> { applyEnrichment(incident, ai); incidentRepository.save(incident); },
                () -> { markFailed(incident); incidentRepository.save(incident); }
        );
    }

    private static void applyEnrichment(IncidentEntity incident, AiEnrichment ai) {
        incident.setAiSummary(ai.summary());
        incident.setAiLikelyCause(ai.likelyCause());
        incident.setAiGeneratedAt(Instant.now());
        incident.setAiSummaryStatus(AiSummaryStatus.COMPLETED);
    }


    private static void markFailed(IncidentEntity incident) {
        incident.setAiSummaryStatus(AiSummaryStatus.FAILED);
    }
}
