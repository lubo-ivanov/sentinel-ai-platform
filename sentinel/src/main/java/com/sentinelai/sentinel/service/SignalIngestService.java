package com.sentinelai.sentinel.service;

import com.sentinelai.sentinel.api.RawSignal;
import com.sentinelai.sentinel.classifier.ClassifierService;
import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import com.sentinelai.sentinel.domain.OperationalEventEntity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import com.sentinelai.sentinel.repository.OperationalEventRepository;
import com.sentinelai.sentinel.repository.RawSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SignalIngestService {

    private final RawSignalRepository repository;
    private final ClassifierService classifierService;
    private final OperationalEventRepository operationalEventRepository;

    public void ingest(String source, RawSignal signal) {
        RawSignalEntity saved = repository.save(new RawSignalEntity(
                UUID.randomUUID(),
                signal.id(),
                source,
                Optional.ofNullable(signal.occurredAt()).orElseGet(Instant::now),
                signal.message(),
                signal.hints()
        ));
        classifierService.classifyAndStore(saved);
    }

    public void recordIngestionFailure(
            String rawPayload,
            Throwable cause,
            String topic,
            int partition,
            long offset
    ) {
        OperationalEventEntity failure = new OperationalEventEntity(
                UUID.randomUUID(),
                null,
                "sentinel",
                Instant.now(),
                FailureType.INGESTION_FAILED,
                Severity.ERROR,
                OperationalEvent.Classification.Method.FAILURE,
                null,
                1.0,
                Map.of("kafka.topic", topic,
                        "kafka.partition", partition,
                        "kafka.offset", offset,
                        "error.class", cause.getClass().getName(),
                        "error.message", cause.getMessage() == null ? "" : cause.getMessage(),
                        "raw.payload", rawPayload == null ? "" : rawPayload
                )
        );
        operationalEventRepository.save(failure);
    }
}
