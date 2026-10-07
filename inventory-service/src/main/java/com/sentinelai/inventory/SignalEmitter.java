package com.sentinelai.inventory;

import com.sentinelai.inventory.signal.RawSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class SignalEmitter {

    private final SidecarClient sidecarClient;
    private final AtomicLong counter = new AtomicLong(0);

    public SignalEmitter(SidecarClient sidecarClient) {
        this.sidecarClient = sidecarClient;
    }

    @Scheduled(fixedDelayString = "${inventory.emit-interval-ms}")
    public void emit() {
        String signalId = UUID.randomUUID().toString();
        long count = counter.incrementAndGet();
        RawSignal payload = count % 2 == 0
                ? new RawSignal(
                signalId,
                Instant.now().toString(),
                "stock reservation timeout",
                Map.of("item", "SKU-123", "warehouse", "EU-WEST"))
                : new RawSignal(
                signalId,
                Instant.now().toString(),
                "stock count mismatch",
                Map.of("item", "SKU-456", "expected", 100, "actual", 73));

        sidecarClient.send(payload);
        log.info("Emitted signal {}", payload.id());
    }
}