package com.voyage.itinerary;

import static org.hamcrest.Matchers.hasSize;
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
class ItineraryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void editorManagesItinerary_viewerReadOnly_optimisticLock_reorder() throws Exception {
        String owner = signupAndLogin("it_owner@voyage.com");
        String editor = signupAndLogin("it_editor@voyage.com");
        String viewer = signupAndLogin("it_viewer@voyage.com");
        String outsider = signupAndLogin("it_outsider@voyage.com");
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "EDITOR", editor);
        inviteAndAccept(owner, tripId, "VIEWER", viewer);

        // editor creates two items on the same day
        JsonNode item1 = createItem(editor, tripId, "2026-08-14", "성산일출봉 메모");
        JsonNode item2 = createItem(editor, tripId, "2026-08-14", "카페 메모");
        long id1 = item1.get("id").asLong();
        long id2 = item2.get("id").asLong();
        long v1 = item1.get("version").asLong();

        // sort order auto-assigned in creation order
        org.junit.jupiter.api.Assertions.assertEquals(0, item1.get("sortOrder").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(1, item2.get("sortOrder").asInt());

        // viewer can read but not create
        mockMvc.perform(get("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(post("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("date", "2026-08-15"))))
                .andExpect(status().isForbidden());

        // outsider cannot even read
        mockMvc.perform(get("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + outsider))
                .andExpect(status().isNotFound());

        // editor updates item1 with the correct version -> version bumps
        mockMvc.perform(patch("/api/itinerary/" + id1).header("Authorization", "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("version", v1, "note", "수정된 메모"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("수정된 메모"))
                .andExpect(jsonPath("$.version").value((int) (v1 + 1)));

        // reusing the stale version -> 409
        mockMvc.perform(patch("/api/itinerary/" + id1).header("Authorization", "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("version", v1, "note", "또 수정"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("C005"));

        // reorder: put item2 before item1; order persists after refetch
        mockMvc.perform(post("/api/itinerary/reorder").header("Authorization", "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("items", List.of(
                                Map.of("itemId", id2, "date", "2026-08-14", "sortOrder", 0),
                                Map.of("itemId", id1, "date", "2026-08-14", "sortOrder", 1))))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) id2))
                .andExpect(jsonPath("$[1].id").value((int) id1));
    }

    private JsonNode createItem(String token, long tripId, String date, String note) throws Exception {
        String body = mockMvc.perform(post("/api/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("date", date, "note", note))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
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
