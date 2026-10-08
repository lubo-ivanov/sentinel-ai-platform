package com.sentinelai.sentinel.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "sentinel.llm.provider", havingValue = "ollama", matchIfMissing = true)
@Slf4j
public class OllamaClient implements  LlmClient {

    private final RestClient restClient;
    private final String model;

    public OllamaClient(
            @Value("${ollama.base-url:http://ollama:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2:3b}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(Duration.ofSeconds(5));
                    setReadTimeout(Duration.ofSeconds(60));
                }})
                .build();
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "format", "json",
                "options", Map.of("temperature", 0)
        );

        ResponseEntity<Map<String, Object>> response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});
        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new IllegalStateException("Empty response from Ollama");
        }

        return (String) body.get("response");
    }
}
