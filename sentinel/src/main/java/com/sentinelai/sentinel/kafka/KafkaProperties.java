package com.sentinelai.sentinel.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(
        String bootstrapServers,
        Consumer consumer,
        List<Topic> topics
) {
    public record Consumer(
            String groupId,
            String autoOffsetReset,
            boolean enableAutoCommit,
            int maxPollRecords,
            Duration pollTimeout,
            Duration pauseBetweenPolls,
            Duration shutdownTimeout,
            int maxAttempts,
            Duration retryBackoff
    ) {
    }

    public record Topic(
            String name,
            int partitions,
            short replicationFactor
    ) {
    }
}
