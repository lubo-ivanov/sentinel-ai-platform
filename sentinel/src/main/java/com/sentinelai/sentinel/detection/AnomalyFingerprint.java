package com.sentinelai.sentinel.detection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

public record AnomalyFingerprint(String value) {
    public static AnomalyFingerprint of(Anomaly anomaly) {
        String raw = anomaly.ruleId() + ":" + anomaly.keys().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + String.valueOf(e.getValue()))
                .collect(Collectors.joining(","));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return new AnomalyFingerprint(HexFormat.of().formatHex(hash).substring(0, 64));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
