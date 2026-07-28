package com.sentinelai.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.payment.signal.RawSignal;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@Component
@Slf4j
public class SignalPublisher {

    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topic;

    public SignalPublisher(KafkaConfig kafkaConfig,
                           ObjectMapper objectMapper,
                           @Value("${sentinel.signals-topic}") String topic) {
        this.producer = new KafkaProducer<>(kafkaConfig.producerProperties());
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public Future<RecordMetadata> publish(RawSignal signal) {
        String json;
        try {
            json = objectMapper.writeValueAsString(signal);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise RawSignal", e);
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, signal.id(), json);
        return producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to publish signal key={} to topic={}", signal.id(), topic, exception);
            } else {
                log.debug("Published signal to {}-{} @offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @PreDestroy
    public void close() {
        log.info("Closing Kafka producer...");
        producer.close();
    }

}
