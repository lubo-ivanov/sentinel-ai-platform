package com.sentinelai.inventory;

import com.sentinelai.inventory.signal.RawSignal;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SidecarClient {

    private final RestClient restClient;

    @PostConstruct
    public void waitForSidecar() throws InterruptedException {
        int attempts = 0;
        while (true) {
            try {
                restClient.get().uri("/health").retrieve().toBodilessEntity();
                log.info("Sidecar is ready after {} attempts", attempts);
                return;
            } catch (Exception e) {
                attempts++;
                log.warn("Sidecar not ready, attempt {}, retrying in 2s...", attempts);
                Thread.sleep(2000);
            }
        }
    }

    public  SidecarClient(@Value("${sidecar.url}") String sidecarUrl) {
        this.restClient = RestClient.builder().baseUrl(sidecarUrl).build();
    }

    public void send(RawSignal signal) {

        try {
            restClient.post()
                    .uri("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(signal)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to send signal id={} to sidecar", signal.id(), e);
        }
    }
}
