package com.sentinelai.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class SignalEmitter {

    private static final String SOURCE_HEADER = "X-Sentinel-Source";
    private static final String ID_PREFIX = "pay-";

    private record Signal(String id, String occurredAt, String message, Hints hints) {}
    private record Hints(String provider, double amount, String currency) {}

    private final RestClient restClient;
    private final String signalsPath;
    private final String sourceName;
    private final AtomicLong counter = new AtomicLong(0);

    public SignalEmitter(
            @Value("${sentinel.url}") String sentinelUrl,
            @Value("${sentinel.signals-path}") String signalsPath,
            @Value("${spring.application.name}") String sourceName
    ) {
        this.restClient = RestClient.builder().baseUrl(sentinelUrl).build();
        this.signalsPath = signalsPath;
        this.sourceName = sourceName;
    }

    @Scheduled(fixedDelayString = "${payment.emit-interval-ms}")
    public void emit() {
        Signal payload = new Signal(
                ID_PREFIX + counter.incrementAndGet(),
                Instant.now().toString(),
                "stripe timeout after 5000ms",
                new Hints("stripe", 42.00, "USD")
        );

        restClient.post()
                .uri(signalsPath)
                .header(SOURCE_HEADER, sourceName)
                .contentType(APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("Emitted signal {}", payload.id());
    }
}
