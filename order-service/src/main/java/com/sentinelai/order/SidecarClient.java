package com.sentinelai.order;

import com.sentinelai.order.signal.RawSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SidecarClient {
    private final RestClient restClient;

    public SidecarClient(@Value("${sidecar.url}") String sidecarUrl) {
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
