package com.sentinelai.sidecar.buffer;

import com.sentinelai.sidecar.health.StateManager;
import com.sentinelai.sidecar.kafka.SignalPublisher;
import com.sentinelai.sidecar.signal.RawSignal;
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

    private volatile boolean running = true;
    private Thread thread;

    @PostConstruct
    public void start() {
        thread = Thread.ofVirtual().name("forwarder").start(this::run);
    }

    private void run() {
        while (running) {
            try {
                RawSignal signal = eventQueue.getQueue().take();
                signalPublisher.publish(signal);
                stateManager.markSuccess();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to publish signal", e);
                stateManager.markFailure();
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
