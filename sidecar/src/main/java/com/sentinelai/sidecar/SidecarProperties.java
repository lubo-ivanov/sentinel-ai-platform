package com.sentinelai.sidecar;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sidecar")
public record SidecarProperties(
   String source,
   String signalsTopic,
   int queueCapacity
) {}
