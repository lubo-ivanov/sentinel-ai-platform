package com.sentinelai.sentinel.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaTopicInitializer {

    private final KafkaConfig kafkaConfig;

    @PostConstruct
    public void ensureTopicsExist() {
        try (AdminClient admin = AdminClient.create(adminProperties())){
            for (KafkaProperties.Topic topic : kafkaConfig.topics()) {
                createIfMissing(admin, toNewTopic(topic));
            }
        }
    }

    private NewTopic toNewTopic(KafkaProperties.Topic topic) {
        return new NewTopic(topic.name(), topic.partitions(), topic.replicationFactor());
    }

    private void createIfMissing(AdminClient admin, NewTopic topic) {
        try {
            admin.createTopics(List.of(topic)).all().get();
            log.info("Created topic {} (partitions={}, replicationFactor={})",
                    topic.name(), topic.numPartitions(), topic.replicationFactor());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.info("Topic {} already exists — skipping creation", topic.name());
                return;
            }
            throw new KafkaTopicInitializationException(
                    "Failed to create topic " + topic.name(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaTopicInitializationException(
                    "Interrupted while creating topic " + topic.name(), e);
        }
    }


    private Properties adminProperties() {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers());
        return p;
    }

    static class KafkaTopicInitializationException extends RuntimeException {
        KafkaTopicInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
