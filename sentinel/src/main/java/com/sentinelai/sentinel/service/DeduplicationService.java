package com.sentinelai.sentinel.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private static final String KEY_PREFIX = "dedup:signal:";

    private final StringRedisTemplate template;
    private final MeterRegistry meterRegistry;

    @Value("${sentinel.dedup.ttl:24h}")
    private Duration ttl;

    public boolean isNew(String externalId, String source) {
        boolean isNew = Boolean.TRUE.equals(
                template.opsForValue().setIfAbsent(KEY_PREFIX + externalId, "1", ttl)
        );
        if (!isNew) {
            meterRegistry.counter("events.deduped", "source", source).increment();
        }

        return isNew;
    }

}
