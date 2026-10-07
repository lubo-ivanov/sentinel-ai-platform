package com.sentinelai.sidecar.controller;

import com.sentinelai.sidecar.SidecarProperties;
import com.sentinelai.sidecar.buffer.EventQueue;
import com.sentinelai.sidecar.service.NormalizationService;
import com.sentinelai.sidecar.signal.RawSignal;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventQueue eventQueue;
    private final NormalizationService normalizationService;
    private final MeterRegistry meterRegistry;
    private final SidecarProperties properties;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@RequestBody RawSignal signal) {
        eventQueue.offer(normalizationService.normalize(signal));
        meterRegistry.counter("events.received", "source", properties.source()).increment();
    }
}
