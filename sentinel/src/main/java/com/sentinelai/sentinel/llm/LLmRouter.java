package com.sentinelai.sentinel.llm;

import com.sentinelai.sentinel.domain.IncidentSeverity;
import org.springframework.stereotype.Component;

@Component
public class LLmRouter {
    private static final String MODEL_LARGE = "llama3.1:8b";
    private static final String MODEL_SMALL = "llama3.2:3b";

    public String modelFor(String severity) {
        return IncidentSeverity.HIGH.name().equals(severity) ? MODEL_LARGE : MODEL_SMALL;
    }
}
