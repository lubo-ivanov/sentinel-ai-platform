package com.sentinelai.sentinel.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IncidentEventPublisher {
    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;

    public IncidentEventPublisher(KafkaConfig config, ObjectMapper objectMapper) {
        this.producer = new KafkaProducer<>(config.producerProperties());
        this.objectMapper = objectMapper;
    }


    public void publish(String topic, String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            producer.send(new ProducerRecord<>(topic, key, json),
                    (meta, ex) -> {
                if (ex != null) log.error("Failed to push to {}", topic, ex);
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise event", e);
        }
    }

    @PreDestroy
    public void close() {
        producer.close();
    }
}
