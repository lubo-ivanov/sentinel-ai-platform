package com.sentinelai.order;

import com.sentinelai.order.kafka.SignalPublisher;
import com.sentinelai.order.signal.RawSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

@Component
@Slf4j
public class SignalEmitter {

    private static final List<String> ORDER_IDS = IntStream.range(0, 10)
            .mapToObj(i -> "order-" + (10000 + ThreadLocalRandom.current().nextInt(90000)))
            .toList();
    private final AtomicLong counter = new AtomicLong(0);

    private final SignalPublisher signalPublisher;
    private final String serviceName;


    public SignalEmitter(SignalPublisher signalPublisher,
                         @Value("${spring.application.name}") String serviceName) {
        this.signalPublisher = signalPublisher;
        this.serviceName = serviceName;
    }


    @Scheduled(fixedDelayString = "${order.emit-interval-ms}")
    public void emit() {
        String signalId = UUID.randomUUID().toString();
        long count = counter.incrementAndGet();
        String orderId = ORDER_IDS.get(ThreadLocalRandom.current().nextInt(ORDER_IDS.size()));
        RawSignal payload = count % 2 == 0
                ? new RawSignal(
                signalId,
                serviceName, Instant.now().toString(),
                "checkout flow slow",
                Map.of("service", "checkout", "duration_ms", 8000))
                : new RawSignal(
                signalId,
                serviceName,
                Instant.now().toString(),
                "unexpected order state transition",
                Map.of("orderId", orderId, "from", "PENDING", "to", "FAILED"));

        signalPublisher.publish(payload);
        log.info("Emitted signal {}", payload.id());
    }
}
