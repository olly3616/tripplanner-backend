package com.voyage.share;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voyage.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class ShareIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void passwordProtectedShare_exposesReadOnlySummary() throws Exception {
        String owner = signupAndLogin("s_owner@voyage.com");
        long ownerId = meId(owner);
        long tripId = createTrip(owner);
        addItinerary(owner, tripId);
        addPlace(owner, tripId);
        addExpense(owner, tripId, ownerId);

        // create a password-protected link that includes expenses
        String body = mockMvc.perform(post("/api/trips/" + tripId + "/share-links")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("password", "secret1", "includeExpenses", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasPassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asString();

        // public access without / with wrong password is rejected
        mockMvc.perform(get("/api/share/" + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("S002"));
        mockMvc.perform(get("/api/share/" + token).param("password", "wrong"))
                .andExpect(status().isUnauthorized());

        // correct password returns the read-only summary (no auth header)
        mockMvc.perform(get("/api/share/" + token).param("password", "secret1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제주 여행"))
                .andExpect(jsonPath("$.itinerary", hasSize(1)))
                .andExpect(jsonPath("$.places", hasSize(1)))
                .andExpect(jsonPath("$.budget.totalBaseMinor").value(30_000));
    }

    @Test
    void noPasswordLink_hidesBudgetWhenNotIncluded_andRevokeWorks() throws Exception {
        String owner = signupAndLogin("s_owner2@voyage.com");
        long tripId = createTrip(owner);

        String body = mockMvc.perform(post("/api/trips/" + tripId + "/share-links")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("includeExpenses", false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode link = objectMapper.readTree(body);
        String token = link.get("token").asString();
        long linkId = link.get("id").asLong();

        mockMvc.perform(get("/api/share/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제주 여행"))
                .andExpect(jsonPath("$.budget").doesNotExist());

        // revoke -> link no longer resolves
        mockMvc.perform(delete("/api/trips/" + tripId + "/share-links/" + linkId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/share/" + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S001"));
    }

    @Test
    void unknownToken_returns404() throws Exception {
        mockMvc.perform(get("/api/share/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S001"));
    }

    private void addItinerary(String token, long tripId) throws Exception {
        mockMvc.perform(post("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("date", "2026-08-14", "note", "성산일출봉"))))
                .andExpect(status().isCreated());
    }

    private void addPlace(String token, long tripId) throws Exception {
        mockMvc.perform(post("/api/trips/" + tripId + "/places").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "카페 A", "address", "제주", "category", "cafe"))))
                .andExpect(status().isCreated());
    }

    private void addExpense(String token, long tripId, long payerId) throws Exception {
        mockMvc.perform(post("/api/trips/" + tripId + "/expenses").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "택시", "amountMinor", 30_000L, "currency", "KRW", "category", "교통",
                                "payerId", payerId, "splitMethod", "EQUAL", "spentOn", "2026-08-14",
                                "participants", List.of(Map.of("userId", payerId))))))
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

    private long meId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
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
