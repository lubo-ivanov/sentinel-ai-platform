package com.sentinelai.payment.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(
        String bootstrapServers,
        Producer producer
) {
    public record Producer(
            String acks,
            boolean enableIdempotence
    ) {}
}
