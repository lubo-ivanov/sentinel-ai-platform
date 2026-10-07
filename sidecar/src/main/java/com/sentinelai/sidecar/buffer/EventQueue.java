package com.sentinelai.sidecar.buffer;

import com.sentinelai.sidecar.SidecarProperties;
import com.sentinelai.sidecar.signal.RawSignal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
@Slf4j
@Getter
public class EventQueue {

    private final BlockingQueue<RawSignal> queue;

    public EventQueue(SidecarProperties properties, MeterRegistry meterRegistry) {
        this.queue = new ArrayBlockingQueue<>(properties.queueCapacity());
        meterRegistry.gauge("buffer.depth",
                Tags.of("source", properties.source()),
                queue,
                Queue::size);
    }

    public void offer(RawSignal signal) {
        if (!queue.offer(signal)) {
            log.warn("Queue full - dropping signal id={}", signal.id());
        }
    }
}
