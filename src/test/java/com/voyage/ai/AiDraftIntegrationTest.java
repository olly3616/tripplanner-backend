package com.voyage.ai;

import static org.hamcrest.Matchers.hasSize;
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
class AiDraftIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void draftSpreadsSavedPlacesAcrossTripDays() throws Exception {
        String owner = signupAndLogin("ai_owner@voyage.com");
        String outsider = signupAndLogin("ai_outsider@voyage.com");
        long tripId = createTrip(owner);
        savePlace(owner, tripId, "성산일출봉");
        savePlace(owner, tripId, "카페 A");

        // 4-day trip (08-14..08-17); default 3/day, 2 places -> both on day 1
        mockMvc.perform(post("/api/trips/" + tripId + "/ai/itinerary-drafts")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(4)))
                .andExpect(jsonPath("$.days[0].items", hasSize(2)))
                .andExpect(jsonPath("$.days[0].items[0].placeName").exists())
                .andExpect(jsonPath("$.days[1].items", hasSize(0)));

        // non-members cannot request a draft
        mockMvc.perform(post("/api/trips/" + tripId + "/ai/itinerary-drafts")
                        .header("Authorization", "Bearer " + outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    private void savePlace(String token, long tripId, String name) throws Exception {
        mockMvc.perform(post("/api/trips/" + tripId + "/places").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "category", "관광"))))
                .andExpect(status().isCreated());
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
