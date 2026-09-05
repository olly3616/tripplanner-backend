package com.voyage.activity;

import static org.hamcrest.Matchers.containsString;
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
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void newExpense_notifiesOtherMembers_andAddsActivity() throws Exception {
        String owner = signupAndLogin("n_owner@voyage.com");
        String member = signupAndLogin("n_member@voyage.com");
        long ownerId = meId(owner);
        long memberId = meId(member);
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "EDITOR", member);

        // member records an expense
        mockMvc.perform(post("/api/trips/" + tripId + "/expenses").header("Authorization", "Bearer " + member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "숙소비", "amountMinor", 240_000L, "currency", "KRW",
                                "payerId", memberId, "splitMethod", "EQUAL", "spentOn", "2026-08-14",
                                "participants", List.of(Map.of("userId", ownerId), Map.of("userId", memberId))))))
                .andExpect(status().isCreated());

        // the owner gets a notification, the actor (member) does not
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("EXPENSE_CREATED"))
                .andExpect(jsonPath("$[0].message").value(containsString("숙소비")))
                .andExpect(jsonPath("$[0].read").value(false));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // activity feed has exactly one entry, visible to members
        String activity = mockMvc.perform(get("/api/trips/" + tripId + "/activity")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("EXPENSE_CREATED"))
                .andReturn().getResponse().getContentAsString();
        objectMapper.readTree(activity); // sanity parse

        // owner marks the notification read
        long notifId = notificationId(owner);
        mockMvc.perform(patch("/api/notifications/" + notifId + "/read").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    private long notificationId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = objectMapper.readTree(body);
        return arr.get(0).get("id").asLong();
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
