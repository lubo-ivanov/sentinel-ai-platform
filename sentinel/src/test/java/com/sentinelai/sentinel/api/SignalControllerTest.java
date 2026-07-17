package com.sentinelai.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.sentinel.service.SignalIngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignalController.class)
class SignalControllerTest {

    private static final String URL = "/api/v1/signals";
    private static final String SOURCE_HEADER = "X-Sentinel-Source";
    private static final String SOURCE = "payment-service";
    private static final RawSignal FULL = new RawSignal(
            "req-abc123",
            Instant.parse("2026-07-17T12:34:56Z"),
            "stripe timeout",
            Map.of("provider", "stripe")
    );
    private static final RawSignal MINIMAL = new RawSignal(null, null, "stripe timeout", null);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean SignalIngestService signalIngestService;

    @Test
    void ingest_withHeader_returns202() throws Exception {
        post(FULL, SOURCE).andExpect(status().isAccepted());
        verify(signalIngestService).ingest(SOURCE, FULL);
    }

    @Test
    void ingest_missingHeader_returns400() throws Exception {
        post(FULL, null).andExpect(status().isBadRequest());
    }

    @Test
    void ingest_minimalBody_returns202() throws Exception {
        post(MINIMAL, SOURCE).andExpect(status().isAccepted());
    }

    private ResultActions post(RawSignal payload, String source) throws Exception {
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload));
        if (source != null) req.header(SOURCE_HEADER, source);
        return mockMvc.perform(req);
    }
}
