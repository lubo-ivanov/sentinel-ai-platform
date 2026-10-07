package com.sentinelai.sidecar.controller;

import com.sentinelai.sidecar.buffer.EventQueue;
import com.sentinelai.sidecar.signal.RawSignal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EventController {

    private EventQueue eventQueue;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@RequestBody RawSignal signal) {
        eventQueue.offer(signal);
    }
}
