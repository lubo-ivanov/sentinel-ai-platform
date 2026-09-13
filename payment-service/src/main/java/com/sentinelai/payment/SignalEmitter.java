package com.sentinelai.payment;

import com.sentinelai.payment.kafka.SignalPublisher;
import com.sentinelai.payment.signal.RawSignal;
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

    private static final String ID_PREFIX = "pay-";

    private final SignalPublisher signalPublisher;
    private final AtomicLong counter = new AtomicLong(0);

    private final String serviceName;

    public SignalEmitter(SignalPublisher signalPublisher,
                         @Value("${spring.application.name}") String serviceName) {
        this.signalPublisher = signalPublisher;
        this.serviceName = serviceName;
    }


    @Scheduled(fixedDelayString = "${payment.emit-interval-ms}")
    public void emit() {
        RawSignal payload = new RawSignal(
                ID_PREFIX + counter.incrementAndGet(),
                serviceName,
                Instant.now().toString(),
                "stripe timeout after 5000ms",
                Map.of("provider", "stripe", "amount", 42.00, "currency", "USD")
        );

        signalPublisher.publish(payload);
        log.info("Emitted signal {}", payload.id());
    }
}
