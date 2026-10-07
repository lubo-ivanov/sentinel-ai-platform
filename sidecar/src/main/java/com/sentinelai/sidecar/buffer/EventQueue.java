package com.sentinelai.sidecar.buffer;

import com.sentinelai.sidecar.SidecarProperties;
import com.sentinelai.sidecar.signal.RawSignal;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
@Slf4j
@Getter
public class EventQueue {

    private final BlockingQueue<RawSignal> queue;

    public EventQueue(SidecarProperties properties) {
        this.queue = new ArrayBlockingQueue<>(properties.queueCapacity());
    }

    public boolean offer(RawSignal signal) {
        boolean accepted = queue.offer(signal);
        if (!accepted) {
            log.warn("Queue full - dropping signal id={}", signal.id());
        }
        return  accepted;
    }
}
