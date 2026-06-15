package com.sentinelai.sentinel.api;

import com.sentinelai.sentinel.service.IncidentStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
@Import(IncidentStore.class)
public class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void findAll() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
