package com.sentinelai.sentinel.api;

import com.sentinelai.sentinel.service.SignalIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalIngestService signalIngestService;

    @PostMapping
    public ResponseEntity<Void> ingest(
            @RequestHeader("X-Sentinel-Source") String source,
            @RequestBody RawSignal signal
    ) {
        signalIngestService.ingest(source, signal);
        return ResponseEntity.accepted().build();
    }
}
