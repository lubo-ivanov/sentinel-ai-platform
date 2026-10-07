package com.sentinelai.sidecar.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class StateManager {

    private final AtomicReference<SidecarState> state = new AtomicReference<>(SidecarState.NORMAL);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    public SidecarState current() {
        return state.get();
    }

    public void markSuccess() {
        failureCount.set(0);
        SidecarState previous = state.getAndUpdate(current ->
                switch (current) {
                    case DEGRADED, DOWN -> SidecarState.RECOVERY;
                    case RECOVERY -> SidecarState.NORMAL;
                    default -> SidecarState.NORMAL;
                });
        if (previous != SidecarState.NORMAL) {
            log.info("Sidecar state: {} -> {}", previous, state.get());
        }
    }

    public void markFailure() {
        int failures = failureCount.incrementAndGet();
        SidecarState current = state.updateAndGet(c ->
                switch (c) {
                    case NORMAL -> failures >= 3 ? SidecarState.DEGRADED : SidecarState.NORMAL;
                    case DEGRADED -> failures >= 10 ? SidecarState.DOWN : SidecarState.DEGRADED;
                    default -> c;
                });
        log.warn("Sidecar state: {} (failures={})", current, failures);
    }
}
