package com.sentinelai.sidecar.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties props;

    public Properties producerProperties() {
        Properties p = new Properties();
        p.put(BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        p.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ACKS_CONFIG, props.producer().acks());
        p.put(ENABLE_IDEMPOTENCE_CONFIG, props.producer().enableIdempotence());
        return p;
    }
}
