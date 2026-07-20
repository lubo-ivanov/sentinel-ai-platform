package com.sentinelai.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SignalEmitterTest {

    private static final String SIGNALS_PATH = "/api/v1/signals";
    private static final String SOURCE_NAME = "payment-service";
    private static final String SOURCE_HEADER = "X-Sentinel-Source";

    private MockWebServer sentinel;
    private SignalEmitter emitter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        sentinel = new MockWebServer();
        sentinel.start();
        emitter = new SignalEmitter(
                sentinel.url("/").toString(),
                SIGNALS_PATH,
                SOURCE_NAME
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        sentinel.shutdown();
    }

    @Test
    void emit_postsToConfiguredPathWithSourceHeader() throws Exception {
        sentinel.enqueue(new MockResponse().setResponseCode(202));

        emitter.emit();

        RecordedRequest request = sentinel.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo(SIGNALS_PATH);
        assertThat(request.getHeader(SOURCE_HEADER)).isEqualTo(SOURCE_NAME);
        assertThat(request.getHeader("Content-Type")).contains("application/json");
    }

    @Test
    void emit_sendsExpectedPayloadShape() throws Exception {
        sentinel.enqueue(new MockResponse().setResponseCode(202));

        emitter.emit();

        JsonNode body = objectMapper.readTree(sentinel.takeRequest().getBody().readUtf8());
        assertThat(body.get("id").asText()).startsWith("pay-");
        assertThat(body.get("occurredAt").asText()).isNotEmpty();
        assertThat(body.get("message").asText()).isEqualTo("payment processed");
        assertThat(body.get("hints").get("amount").asDouble()).isEqualTo(42.00);
        assertThat(body.get("hints").get("currency").asText()).isEqualTo("USD");
    }

    @Test
    void emit_incrementsIdAcrossCalls() throws Exception {
        sentinel.enqueue(new MockResponse().setResponseCode(202));
        sentinel.enqueue(new MockResponse().setResponseCode(202));
        sentinel.enqueue(new MockResponse().setResponseCode(202));

        emitter.emit();
        emitter.emit();
        emitter.emit();

        String id1 = idFromRequest(sentinel.takeRequest());
        String id2 = idFromRequest(sentinel.takeRequest());
        String id3 = idFromRequest(sentinel.takeRequest());

        assertThat(id1).isEqualTo("pay-1");
        assertThat(id2).isEqualTo("pay-2");
        assertThat(id3).isEqualTo("pay-3");
    }

    private String idFromRequest(RecordedRequest request) throws IOException {
        return objectMapper.readTree(request.getBody().readUtf8()).get("id").asText();
    }
}
