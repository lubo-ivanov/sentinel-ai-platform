package com.sentinelai.order;

import com.sentinelai.order.kafka.SignalPublisher;
import com.sentinelai.order.signal.RawSignal;
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
    private static final String ID_PREFIX = "order-";

    private final SignalPublisher signalPublisher;
    private final AtomicLong counter = new AtomicLong(0);

    private final String serviceName;

    public SignalEmitter(SignalPublisher signalPublisher,
                         @Value("${spring.application.name}") String serviceName) {
        this.signalPublisher = signalPublisher;
        this.serviceName = serviceName;
    }


    @Scheduled(fixedDelayString = "${order.emit-interval-ms}")
    public void emit() {
        long count = counter.incrementAndGet();
        RawSignal payload = count % 2 == 0
                ? new RawSignal(
                ID_PREFIX + count,
                serviceName, Instant.now().toString(),
                "checkout flow slow",
                Map.of("service", "checkout", "duration_ms", 8000))
                : new RawSignal(
                ID_PREFIX + count,
                serviceName,
                Instant.now().toString(),
                "unexpected order state transition",
                Map.of("orderId", "order-" + count, "from", "PENDING", "to", "FAILED"));

        signalPublisher.publish(payload);
        log.info("Emitted signal {}", payload.id());
    }
}
