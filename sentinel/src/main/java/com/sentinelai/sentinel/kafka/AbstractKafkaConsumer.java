package com.sentinelai.sentinel.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractKafkaConsumer<V> {
    protected final KafkaConfig kafkaConfig;
    protected final ObjectMapper objectMapper;

    private KafkaConsumer<String, String> consumer;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    protected AbstractKafkaConsumer(KafkaConfig kafkaConfig, ObjectMapper objectMapper) {
        this.kafkaConfig = kafkaConfig;
        this.objectMapper = objectMapper;
    }

    protected abstract String topicName();

    protected abstract Class<V> valueType();

    protected abstract void process(V value, ConsumerRecord<String, String> raw);

    protected abstract void onProcessingFailed(ConsumerRecord<String, String> record, Throwable cause);

    @PostConstruct
    final void start() {
        String topic = kafkaConfig.topic(topicName()).name();
        consumer = new KafkaConsumer<>(kafkaConfig.consumerProperties());
        consumer.subscribe(List.of(topic));
        log.info("{} subscribed to topic {}", getClass().getSimpleName(), topic);

        running.set(true);
        executor = createConsumerExecutor(topic);
        executor.submit(this::runPollLoop);
    }

    @PreDestroy
    final void stop() {
        log.info("Closing {}...", getClass().getSimpleName());
        signalLoopToStop();
        awaitExecutorShutdown();
    }

    private void runPollLoop() {
        log.info("{} polling loop started", getClass().getSimpleName());
        try {
            while (running.get()) {
                pollOnce();
                Thread.sleep(kafkaConfig.pauseBetweenPolls());
            }
        } catch (WakeupException e) {
            log.info("{} received wakeup - shutting down", getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("{} polling loop failed", getClass().getSimpleName(), e);
        } finally {
            consumer.close();
            log.info("{} polling loop exited, consumer closed", getClass().getSimpleName());
        }
    }

    private void pollOnce() {
        ConsumerRecords<String, String> records = consumer.poll(kafkaConfig.pollTimeout());
        if (records.isEmpty()) return;
        log.info("Polled {} records from {}", records.count(), topicName());

        try {
            for (ConsumerRecord<String, String> record : records) {
                handleRecord(record);
            }
            consumer.commitSync();
        } catch (Exception e) {
            log.error("Batch processing halted before commit — will retry on next poll", e);
        }

    }

    private void handleRecord(ConsumerRecord<String, String> record) {
        int maxAttempts = kafkaConfig.maxAttempts();
        long backoffMs = kafkaConfig.retryBackoff().toMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                V value = objectMapper.readValue(record.value(), valueType());
                process(value, record);
                return;
            } catch (JsonProcessingException e) {
                log.error("Deserialization failed for offset={} — no retry", record.offset(), e);
                onProcessingFailed(record, e);
                return;
            } catch (Exception e) {
                log.warn("Processing attempt {}/{} failed for offset={}: {}",
                        attempt, maxAttempts, record.offset(), e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("Exhausted {} attempts for offset={}", maxAttempts, record.offset(), e);
                    onProcessingFailed(record, e);
                    return;
                }
                sleepQuietly(backoffMs);
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void signalLoopToStop() {
        running.set(false);
        if (consumer != null) {
            consumer.wakeup();
        }
    }

    private void awaitExecutorShutdown() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(kafkaConfig.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("Executor didn't terminate in {}, forcing shutdown", kafkaConfig.shutdownTimeout());
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static ExecutorService createConsumerExecutor(String topicName) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kafka-consumer-" + topicName);
            t.setDaemon(false);
            return t;
        });
    }
}
