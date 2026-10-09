package com.sentinelai.sentinel.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sentinel.llm.provider", havingValue = "mock")
@Slf4j
public class MockLlmClient implements  LlmClient {
    @Override
    public String generate(String prompt, String model, boolean jsonMode) {
        log.debug("MockLlmClient returning canned response");
        return """
                {
                  "summary": "Mock incident summary — LLM provider is set to mock mode.",
                  "likely_cause": "Mock likely cause — no real LLM call was made.",
                  "severity_assessment": "Mock severity assessment."
                }
                """;
    }
}
