package com.sentinelai.inventory;

import com.sentinelai.inventory.kafka.SignalPublisher;
import com.sentinelai.inventory.signal.RawSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class SignalEmitter {

    private static final String ID_PREFIX = "inv-";

    private final SignalPublisher signalPublisher;
    private final String serviceName;
    private final AtomicLong counter = new AtomicLong(0);

    public SignalEmitter(SignalPublisher signalPublisher,
                         @Value("${spring.application.name}") String serviceName) {
        this.signalPublisher = signalPublisher;
        this.serviceName = serviceName;
    }

    @Scheduled(fixedDelayString = "${inventory.emit-interval-ms}")
    public void emit() {
        long count = counter.incrementAndGet();
        RawSignal payload = count % 2 == 0
                ? new RawSignal(
                        ID_PREFIX + count,
                        serviceName,
                        Instant.now().toString(),
                        "stock reservation timeout",
                        Map.of("item", "SKU-123", "warehouse", "EU-WEST"))
                : new RawSignal(
                        ID_PREFIX + count,
                        serviceName,
                        Instant.now().toString(),
                        "stock count mismatch",
                        Map.of("item", "SKU-456", "expected", 100, "actual", 73));

        signalPublisher.publish(payload);
        log.info("Emitted signal {}", payload.id());
    }
}