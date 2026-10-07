package com.sentinelai.sidecar.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class StateManager {

    private final AtomicReference<SidecarState> state = new AtomicReference<>(SidecarState.NORMAL);

    public SidecarState current() {
        return state.get();
    }

    public void markSuccess() {
        SidecarState previous = state.getAndSet(SidecarState.NORMAL);
        if (previous != SidecarState.NORMAL) {
            log.info("Sidecar state: {} -> NORMAL", previous);
        }
    }

    public void markFailure() {
        state.updateAndGet(current ->
                switch (current) {
                    case NORMAL -> SidecarState.DEGRADED;
                    case DEGRADED -> SidecarState.DOWN;
                    default -> current;
        });
        log.warn("Sidecar state: {} -> RECOVERY", state.get());
    }

    public void markRecovery() {
        SidecarState previous = state.getAndSet(SidecarState.RECOVERY);
        log.info("Sidecar state: {} -> RECOVERY", previous);
    }
}
