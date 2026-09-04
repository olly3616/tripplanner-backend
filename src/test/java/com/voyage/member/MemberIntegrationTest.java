package com.voyage.member;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class MemberIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void inviteAcceptRoleRemove_fullFlow() throws Exception {
        String owner = signupAndLogin("m_owner@voyage.com");
        String guest = signupAndLogin("m_guest@voyage.com");
        long guestId = meId(guest);
        long tripId = createTrip(owner);

        // guest is not a member yet -> hidden
        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + guest))
                .andExpect(status().isNotFound());

        // owner invites as EDITOR
        String inviteBody = mockMvc.perform(post("/api/trips/" + tripId + "/members")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "m_guest@voyage.com", "role", "EDITOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();
        String inviteToken = objectMapper.readTree(inviteBody).get("token").asString();

        // guest accepts -> becomes EDITOR
        mockMvc.perform(post("/api/invitations/accept")
                        .header("Authorization", "Bearer " + guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", inviteToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value((int) tripId))
                .andExpect(jsonPath("$.role").value("EDITOR"));

        // guest can now see the trip; members list has 2
        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trips/" + tripId + "/members").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // a non-owner member cannot invite
        mockMvc.perform(post("/api/trips/" + tripId + "/members")
                        .header("Authorization", "Bearer " + guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "VIEWER"))))
                .andExpect(status().isForbidden());

        // owner changes role, then removes the member
        mockMvc.perform(patch("/api/trips/" + tripId + "/members/" + guestId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));
        mockMvc.perform(delete("/api/trips/" + tripId + "/members/" + guestId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        // removed member loses access
        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + guest))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokedInvitation_cannotBeAccepted() throws Exception {
        String owner = signupAndLogin("m_owner2@voyage.com");
        String guest = signupAndLogin("m_guest2@voyage.com");
        long tripId = createTrip(owner);

        String inviteBody = mockMvc.perform(post("/api/trips/" + tripId + "/members")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "VIEWER"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode invite = objectMapper.readTree(inviteBody);
        long invitationId = invite.get("invitationId").asLong();
        String token = invite.get("token").asString();

        mockMvc.perform(delete("/api/trips/" + tripId + "/invitations/" + invitationId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/invitations/accept")
                        .header("Authorization", "Bearer " + guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("M001"));
    }

    @Test
    void accept_invalidToken_returns400() throws Exception {
        String guest = signupAndLogin("m_guest3@voyage.com");
        mockMvc.perform(post("/api/invitations/accept")
                        .header("Authorization", "Bearer " + guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "does-not-exist"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("M001"));
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
