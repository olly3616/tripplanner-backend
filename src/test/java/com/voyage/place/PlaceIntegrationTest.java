package com.voyage.place;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class PlaceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void search_save_dedup_filter_permissions() throws Exception {
        String owner = signupAndLogin("p_owner@voyage.com");
        String viewer = signupAndLogin("p_viewer@voyage.com");
        String outsider = signupAndLogin("p_outsider@voyage.com");
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "VIEWER", viewer);

        // search via stub provider
        mockMvc.perform(get("/api/trips/" + tripId + "/places/search?query=성산")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].providerPlaceId").value("stub-성산-1"));

        // save a place from the search result
        long placeId = savePlace(owner, tripId, "stub-성산-1", "성산일출봉");

        // saving the same provider place again is a dedup -> same id
        long again = savePlace(owner, tripId, "stub-성산-1", "성산일출봉");
        org.junit.jupiter.api.Assertions.assertEquals(placeId, again);

        // viewer can read, cannot save
        mockMvc.perform(get("/api/trips/" + tripId + "/places").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(post("/api/trips/" + tripId + "/places").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "몰래 저장"))))
                .andExpect(status().isForbidden());

        // outsider cannot access at all
        mockMvc.perform(get("/api/trips/" + tripId + "/places").header("Authorization", "Bearer " + outsider))
                .andExpect(status().isNotFound());

        // update status + tags, then filter
        mockMvc.perform(patch("/api/trips/" + tripId + "/places/" + placeId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("status", "CONFIRMED", "tags", List.of("일출", "명소")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/trips/" + tripId + "/places?status=CONFIRMED")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/trips/" + tripId + "/places?tag=일출")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/trips/" + tripId + "/places?status=WISH")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deletingPlace_nullsItineraryLink() throws Exception {
        String owner = signupAndLogin("p_owner2@voyage.com");
        long tripId = createTrip(owner);
        long placeId = savePlace(owner, tripId, "stub-abc-1", "카페");

        // create an itinerary item linked to the place
        mockMvc.perform(post("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("date", "2026-08-14", "placeId", placeId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placeId").value((int) placeId));

        // deleting the place leaves the item but clears its place link (FK ON DELETE SET NULL)
        mockMvc.perform(delete("/api/trips/" + tripId + "/places/" + placeId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].placeId").doesNotExist());
    }

    private long savePlace(String token, long tripId, String providerPlaceId, String name) throws Exception {
        String body = mockMvc.perform(post("/api/trips/" + tripId + "/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "provider", "STUB", "providerPlaceId", providerPlaceId,
                                "name", name, "address", "제주", "category", "관광"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void inviteAndAccept(String owner, long tripId, String role, String memberToken) throws Exception {
        String inviteBody = mockMvc.perform(post("/api/trips/" + tripId + "/members")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(inviteBody).get("token").asString();
        mockMvc.perform(post("/api/invitations/accept").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk());
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
