package com.sentinelai.sidecar.service;

import com.sentinelai.sidecar.SidecarProperties;
import com.sentinelai.sidecar.signal.RawSignal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NormalizationService {

    private final SidecarProperties properties;

    public RawSignal normalize(RawSignal signal) {
        return new RawSignal(
                signal.id(),
                properties.source(),
                signal.occurredAt() == null ? Instant.now().toString() : signal.occurredAt(),
                signal.message(),
                signal.hints()
        );
    }
}
