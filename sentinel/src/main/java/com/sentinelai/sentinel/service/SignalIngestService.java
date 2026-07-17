package com.sentinelai.sentinel.service;

import com.sentinelai.sentinel.api.RawSignal;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import com.sentinelai.sentinel.repository.RawSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SignalIngestService {

    private final RawSignalRepository repository;

    public void ingest(String source, RawSignal signal) {
        repository.save(new RawSignalEntity(
                UUID.randomUUID(),
                signal.id(),
                source,
                Optional.ofNullable(signal.occurredAt()).orElseGet(Instant::now),
                signal.message(),
                signal.hints()
        ));
    }
}
