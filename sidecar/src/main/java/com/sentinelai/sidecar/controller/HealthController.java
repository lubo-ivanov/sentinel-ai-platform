package com.sentinelai.sidecar.controller;

import com.sentinelai.sidecar.buffer.EventQueue;
import com.sentinelai.sidecar.health.StateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final StateManager stateManager;
    private final EventQueue eventQueue;

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "state", stateManager.current(),
                "bufferDepth", eventQueue.getQueue().size()
        );
    }
}
