package com.sentinelai.payment;

import com.sentinelai.payment.signal.RawSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class SignalEmitter {

    private final SidecarClient sidecarClient;

    public SignalEmitter(SidecarClient sidecarClient) {
        this.sidecarClient = sidecarClient;
    }


    @Scheduled(fixedDelayString = "${payment.emit-interval-ms}")
    public void emit() {
        RawSignal payload = new RawSignal(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                "stripe timeout after 5000ms",
                Map.of("provider", "stripe", "amount", 42.00, "currency", "USD")
        );

        sidecarClient.send(payload);
        log.info("Emitted signal {}", payload.id());
    }
}
