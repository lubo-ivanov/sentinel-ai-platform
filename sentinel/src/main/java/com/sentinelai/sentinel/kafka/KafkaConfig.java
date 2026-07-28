package com.sentinelai.sentinel.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties props;

    public String bootstrapServers() {
        return props.bootstrapServers();
    }

    public List<KafkaProperties.Topic> topics() {
        return props.topics();
    }

    public Duration pollTimeout() {
        return props.consumer().pollTimeout();
    }

    public Duration pauseBetweenPolls() {
        return props.consumer().pauseBetweenPolls();
    }

    public Duration shutdownTimeout() {
        return props.consumer().shutdownTimeout();
    }

    public KafkaProperties.Topic topic(String name) {
        return topics().stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Topic '" + name + "' not configured in kafka.topics"));
    }

    public Properties consumerProperties() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, props.consumer().groupId());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, props.consumer().autoOffsetReset());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, props.consumer().enableAutoCommit());
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, props.consumer().maxPollRecords());
        return p;
    }
}

