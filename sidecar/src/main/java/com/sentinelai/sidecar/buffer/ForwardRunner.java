package com.sentinelai.sidecar.buffer;

import com.sentinelai.sidecar.SidecarProperties;
import com.sentinelai.sidecar.health.SidecarState;
import com.sentinelai.sidecar.health.StateManager;
import com.sentinelai.sidecar.kafka.SignalPublisher;
import com.sentinelai.sidecar.signal.RawSignal;
import com.sentinelai.sidecar.spill.SpillManager;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForwardRunner {

    private final EventQueue eventQueue;
    private final SignalPublisher signalPublisher;
    private final StateManager stateManager;
    private final SpillManager spillManager;
    private final MeterRegistry meterRegistry;
    private final SidecarProperties properties;

    private volatile boolean running = true;
    private Thread thread;

    @PostConstruct
    public void start() {
        thread = Thread.ofVirtual().name("forwarder").start(this::run);
    }

    private void run() {
        while (running) {
            RawSignal signal = null;
            try {
                signal = eventQueue.getQueue().take();
                signalPublisher.publish(signal);
                stateManager.markSuccess();
                meterRegistry.counter("events.published", "source", properties.source()).increment();
                if (stateManager.current() == SidecarState.RECOVERY) {
                    spillManager.drain().forEach(eventQueue::offer);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to publish signal", e);
                stateManager.markFailure();
                if (signal != null) {
                    spillManager.spill(signal);
                }
            }
        }
        log.info("Forwarder stopped");
    }

    @PreDestroy
    public void stop() {
        running = false;
        thread.interrupt();
    }
}
