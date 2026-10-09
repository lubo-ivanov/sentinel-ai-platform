package com.sentinelai.sentinel.llm;

public interface LlmClient {
    String generate(String prompt, String model, boolean jsonMode);
}
