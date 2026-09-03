package com.voyage.trip;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voyage.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class TripIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void tripLifecycle_ownerManages_outsiderCannotSee() throws Exception {
        String owner = signupAndLogin("owner1@voyage.com");
        String outsider = signupAndLogin("outsider1@voyage.com");

        // create
        String created = mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("제주 여름 여행", "2026-08-14", "2026-08-17")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andReturn().getResponse().getContentAsString();
        long tripId = objectMapper.readTree(created).get("id").asLong();

        // owner can read it
        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제주 여름 여행"));

        // outsider cannot see it (existence hidden -> 404)
        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/trips").header("Authorization", "Bearer " + outsider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // owner changes status and title
        mockMvc.perform(patch("/api/trips/" + tripId + "/status")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(patch("/api/trips/" + tripId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "제주 가을 여행"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제주 가을 여행"));

        // delete -> gone
        mockMvc.perform(delete("/api/trips/" + tripId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsOnlyMyTrips() throws Exception {
        String owner = signupAndLogin("owner2@voyage.com");

        mockMvc.perform(post("/api/trips").header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTripJson("도쿄 여행", "2026-10-03", "2026-10-05")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/trips").header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTripJson("부산 여행", "2026-09-01", "2026-09-03")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/trips").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$[0].memberCount").value(1));
    }

    @Test
    void create_endDateBeforeStartDate_returns400() throws Exception {
        String owner = signupAndLogin("owner3@voyage.com");

        mockMvc.perform(post("/api/trips").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("잘못된 여행", "2026-08-17", "2026-08-14")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("무단 여행", "2026-08-14", "2026-08-17")))
                .andExpect(status().isUnauthorized());
    }

    private String createTripJson(String title, String startsOn, String endsOn) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "destination", "제주",
                "startsOn", startsOn,
                "endsOn", endsOn,
                "baseCurrency", "KRW",
                "timezone", "Asia/Seoul"));
    }

    /** Signs up and logs in via the real auth endpoints, returning an access token. */
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
        JsonNode node = objectMapper.readTree(body);
        return node.get("accessToken").asString();
    }
}
