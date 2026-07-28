package com.sentinelai.sentinel.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.api.RawSignal;
import com.sentinelai.sentinel.service.SignalIngestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RawSignalConsumer extends AbstractKafkaConsumer<RawSignal> {

    private static final String TOPIC = "signals.raw";

    private final SignalIngestService ingestService;

    public RawSignalConsumer(
            KafkaConfig kafkaConfig,
            ObjectMapper objectMapper,
            SignalIngestService ingestService
    ) {
        super(kafkaConfig, objectMapper);
        this.ingestService = ingestService;
    }

    @Override
    protected String topicName() {
        return TOPIC;
    }

    @Override
    protected Class<RawSignal> valueType() {
        return RawSignal.class;
    }

    @Override
    protected void process(RawSignal signal, ConsumerRecord<String, String> raw) {
        ingestService.ingest("payment-service", signal);
        log.debug("Ingested signal id={} from offset={}", signal.id(), raw.offset());
    }
}
