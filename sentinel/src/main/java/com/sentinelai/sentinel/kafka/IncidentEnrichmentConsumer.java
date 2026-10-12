package com.sentinelai.sentinel.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.detection.Anomaly;
import com.sentinelai.sentinel.detection.correlation.IncidentEnrichmentMessage;
import com.sentinelai.sentinel.domain.IncidentEnrichmentEntity;
import com.sentinelai.sentinel.domain.IncidentEntity;
import com.sentinelai.sentinel.llm.AiEnrichment;
import com.sentinelai.sentinel.llm.AiSummaryStatus;
import com.sentinelai.sentinel.llm.IncidentEnrichmentService;
import com.sentinelai.sentinel.repository.IncidentEnrichmentRepository;
import com.sentinelai.sentinel.repository.IncidentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class IncidentEnrichmentConsumer extends AbstractKafkaConsumer<IncidentEnrichmentMessage> {

    private static final String TOPIC = "incidents.enrichment";
    private static final String METRIC_ENRICHMENT_DURATION = "incident.enrichment.duration";
    private static final String STATUS = "status";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    private final IncidentRepository incidentRepository;
    private final IncidentEnrichmentService enrichmentService;
    private final MeterRegistry meterRegistry;
    private final IncidentEnrichmentRepository enrichmentRepository;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    protected IncidentEnrichmentConsumer(
            KafkaConfig kafkaConfig,
            ObjectMapper objectMapper,
            IncidentRepository incidentRepository,
            IncidentEnrichmentService enrichmentService,
            MeterRegistry meterRegistry, IncidentEnrichmentRepository incidentEnrichmentRepository) {
        super(kafkaConfig, objectMapper);
        this.incidentRepository = incidentRepository;
        this.enrichmentService = enrichmentService;
        this.meterRegistry = meterRegistry;
        this.enrichmentRepository = incidentEnrichmentRepository;
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
        virtualThreadExecutor.submit(() -> enrichAndSave(message.incidentId(), message.anomaly()));
    }

    @Override
    protected void onProcessingFailed(ConsumerRecord<String, String> record, Throwable cause) {
        log.error("Enrichment processing failed for offset={}", record.offset(), cause);
    }

    protected void enrichAndSave(UUID incidentId, Anomaly anomaly) {
        do {
            waitForEnrichmentSlot(incidentId);
        } while (!claimEnrichment(incidentId));

        IncidentEntity incident = incidentRepository.findById(incidentId).orElse(null);
        IncidentEnrichmentEntity enrichmentEntity= enrichmentRepository.findByIncidentId(incidentId).orElseThrow();

        if (incident == null) {
            log.warn("Incident not found for enrichment id={}", incidentId);
            markFailed(enrichmentEntity);
            enrichmentRepository.save(enrichmentEntity);
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        try {
            enrichmentService.enrich(incident, anomaly).ifPresentOrElse(
                    aiEnrichmentResponse -> {
                        applyEnrichment(enrichmentEntity, aiEnrichmentResponse, succeeded);
                        addRemediation(enrichmentEntity, incident, anomaly, aiEnrichmentResponse);
                    },
                    () -> markFailed(enrichmentEntity)
            );
            enrichmentRepository.save(enrichmentEntity);
        } finally {
            sample.stop(Timer.builder(METRIC_ENRICHMENT_DURATION)
                    .tag(STATUS, succeeded.get() ? STATUS_COMPLETED : STATUS_FAILED)
                    .register(meterRegistry)
            );
        }
    }


    private boolean claimEnrichment(UUID incidentId) {
        try {
            IncidentEnrichmentEntity enrichment = enrichmentRepository.findByIncidentId(incidentId)
                    .orElse(new IncidentEnrichmentEntity(incidentId, AiSummaryStatus.IN_PROGRESS));
            enrichment.setAiSummaryStatus(AiSummaryStatus.IN_PROGRESS);
            enrichmentRepository.save(enrichment);
            return true;
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            log.info("Enrichment claim lost race for incident={}, retrying", incidentId);
            return false;
        }
    }

    private void waitForEnrichmentSlot(UUID incidentId) {
        for (int i = 1; i <= 3; i++) {
            Optional<IncidentEnrichmentEntity> existing = enrichmentRepository.findByIncidentId(incidentId);
            if (existing.isEmpty() || existing.get().getAiSummaryStatus() != AiSummaryStatus.IN_PROGRESS) {
                return;
            }
            log.info("Enrichment IN_PROGRESS for incident={}, poll{}/3", incidentId, i);
            sleep(Duration.ofSeconds(20));
        }

        enrichmentRepository.findByIncidentId(incidentId).ifPresent(enrichment -> {
            enrichment.setAiSummaryStatus(AiSummaryStatus.FAILED);
            enrichmentRepository.save(enrichment);
        });
        sleep(Duration.ofSeconds(10));
    }

    private void applyEnrichment(IncidentEnrichmentEntity enrichmentEntity, AiEnrichment ai, AtomicBoolean succeeded) {
            enrichmentEntity.setAiSummary(ai.summary());
            enrichmentEntity.setAiLikelyCause(ai.likelyCause());
            enrichmentEntity.setAiSummaryStatus(AiSummaryStatus.COMPLETED);
            succeeded.set(true);
    }

    private void markFailed(IncidentEnrichmentEntity enrichmentEntity) {
            enrichmentEntity.setAiSummaryStatus(AiSummaryStatus.FAILED);
    }

    private void addRemediation(IncidentEnrichmentEntity enrichmentEntity, IncidentEntity incident, Anomaly anomaly, AiEnrichment aiEnrichment) {
        enrichmentService.suggestRemediation(incident, anomaly, aiEnrichment)
                .ifPresent(enrichmentEntity::setRemediationSteps);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
