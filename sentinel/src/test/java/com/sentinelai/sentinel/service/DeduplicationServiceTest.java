package com.sentinelai.sentinel.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    private static final String SIGNAL_ID = "3f7a2b1c-0000-0000-0000-000000000001";
    private static final String SOURCE = "payment-service";
    private static final String REDIS_KEY = "dedup:signal:" + SIGNAL_ID;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ValueOperations<String, String> valueOps;

    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        deduplicationService = new DeduplicationService(redis, meterRegistry);
        ReflectionTestUtils.setField(deduplicationService, "ttl", Duration.ofHours(24));
    }

    @Test
    void firstSeen_returnsTrue() {
        when(valueOps.setIfAbsent(eq(REDIS_KEY), anyString(), any(Duration.class))).thenReturn(true);

        assertThat(deduplicationService.isNew(SIGNAL_ID, SOURCE)).isTrue();
    }

    @Test
    void duplicate_returnsFalse() {
        when(valueOps.setIfAbsent(eq(REDIS_KEY), anyString(), any(Duration.class))).thenReturn(false);
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter("events.deduped", "source", SOURCE)).thenReturn(counter);

        assertThat(deduplicationService.isNew(SIGNAL_ID, SOURCE)).isFalse();
    }

    @Test
    void duplicate_incrementsCounter() {
        when(valueOps.setIfAbsent(eq(REDIS_KEY), anyString(), any(Duration.class))).thenReturn(false);
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter("events.deduped", "source", SOURCE)).thenReturn(counter);

        deduplicationService.isNew(SIGNAL_ID, SOURCE);

        verify(counter).increment();
    }

    @Test
    void firstSeen_doesNotIncrementCounter() {
        when(valueOps.setIfAbsent(eq(REDIS_KEY), anyString(), any(Duration.class))).thenReturn(true);

        deduplicationService.isNew(SIGNAL_ID, SOURCE);

        verify(meterRegistry, never()).counter(anyString(), anyString(), anyString());
    }

    @Test
    void fiveCallsSameId_onlyFirstIsNew() {
        when(valueOps.setIfAbsent(eq(REDIS_KEY), anyString(), any(Duration.class)))
                .thenReturn(true, false, false, false, false);
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter("events.deduped", "source", SOURCE)).thenReturn(counter);

        boolean first = deduplicationService.isNew(SIGNAL_ID, SOURCE);
        boolean second = deduplicationService.isNew(SIGNAL_ID, SOURCE);
        boolean third = deduplicationService.isNew(SIGNAL_ID, SOURCE);
        boolean fourth = deduplicationService.isNew(SIGNAL_ID, SOURCE);
        boolean fifth = deduplicationService.isNew(SIGNAL_ID, SOURCE);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isFalse();
        assertThat(fourth).isFalse();
        assertThat(fifth).isFalse();
        verify(counter, times(4)).increment();
    }
}
