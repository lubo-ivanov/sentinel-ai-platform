package com.sentinelai.sentinel.detection.counter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlidingWindowCounter {

    private final StringRedisTemplate redis;

    public void record(String key, Instant now, Duration window) {
        String zsetKey = zsetKey(key);
        String uniqueMember = UUID.randomUUID().toString();
        double score = now.toEpochMilli();

        redis.opsForZSet().add(zsetKey, uniqueMember, score);
        redis.expire(zsetKey, window.plusSeconds(10));
    }

    public long count(String key, Instant now, Duration window) {
        String zsetKey = zsetKey(key);
        double windowStart = now.minus(window).toEpochMilli();
        double windowEnd = now.toEpochMilli();

        redis.opsForZSet().removeRangeByScore(zsetKey, 0, windowStart - 1);

        Long count = redis.opsForZSet().count(zsetKey, windowStart, windowEnd);
        return count == null ? 0 : count;
    }

    private String zsetKey(String key) {
        return "counter:" + key;
    }
}
