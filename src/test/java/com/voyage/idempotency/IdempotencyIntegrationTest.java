package com.voyage.idempotency;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voyage.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void retriedPostWithSameKey_returnsOriginalResponse_noDuplicate() throws Exception {
        String owner = signupAndLogin("idem_owner@voyage.com");
        long tripId = createTrip(owner);
        String payload = objectMapper.writeValueAsString(Map.of("date", "2026-08-14", "note", "성산일출봉"));

        long firstId = createItineraryWithKey(owner, tripId, "key-1", payload);
        long replayId = createItineraryWithKey(owner, tripId, "key-1", payload);
        org.junit.jupiter.api.Assertions.assertEquals(firstId, replayId, "replay must return the original id");

        // only one item was actually created
        mockMvc.perform(get("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // a different key creates a new item; no key also creates a new item
        createItineraryWithKey(owner, tripId, "key-2", payload);
        mockMvc.perform(post("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    private long createItineraryWithKey(String token, long tripId, String key, String payload) throws Exception {
        String body = mockMvc.perform(post("/api/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createTrip(String token) throws Exception {
        String body = mockMvc.perform(post("/api/trips").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "제주 여행", "startsOn", "2026-08-14", "endsOn", "2026-08-17",
                                "baseCurrency", "KRW", "timezone", "Asia/Seoul"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String signupAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "password1", "name", "Tester"))))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "password1"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }
}
