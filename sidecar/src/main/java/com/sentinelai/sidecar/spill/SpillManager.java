package com.sentinelai.sidecar.spill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sidecar.SidecarProperties;
import com.sentinelai.sidecar.signal.RawSignal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpillManager {

    private final ObjectMapper objectMapper;
    private final SidecarProperties properties;
    private Path spillFile;

    @PostConstruct
    public void init() throws IOException {
        Path dir = Path.of(properties.spillDir());
        Files.createDirectories(dir);
        spillFile = dir.resolve("spill.jsonl");
    }

    public void spill(RawSignal signal) {
        try {
            if(Files.exists(spillFile) && Files.size(spillFile) >= properties.maxSpillBytes()) {
                log.warn("Disc cap reached - dropping signal id={}", signal.id());
            }
            String line = objectMapper.writeValueAsString(signal);
            Files.writeString(spillFile, line + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            log.error("Failed to spill signal id={}", signal.id(), e);
        }
    }

    public List<RawSignal> drain() {
        if(!Files.exists(spillFile)) {
            return Collections.emptyList();
        }

        try {
            List<String> lines = Files.readAllLines(spillFile);
            List<RawSignal> signals = lines.stream()
                    .map(this::deserialize)
                    .filter(Objects::nonNull)
                    .toList();
            Files.delete(spillFile);
            return  signals;
        } catch (IOException e) {
            log.error("Failed to drain spill file", e);
            return Collections.emptyList();
        }
    }

    public long spillCount() {
        if (!Files.exists(spillFile)) {
            return 0;
        }
        try (Stream<String> lines = Files.lines(spillFile)) {
            return lines.count();
        } catch (IOException e) {
            return 0;
        }
    }

    @Nullable
    private RawSignal deserialize(String line) {
        try {
            return objectMapper.readValue(line, RawSignal.class);
        } catch (IOException e) {
            log.error("Failed to deserialize spilled signal", e);
            return null;
        }
    }

}
