package com.voyage.poll;

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
class PollIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createVoteChangeVote_singleChoice() throws Exception {
        String owner = signupAndLogin("v_owner@voyage.com");
        String viewer = signupAndLogin("v_viewer@voyage.com");
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "VIEWER", viewer);

        JsonNode poll = createPoll(owner, tripId,
                Map.of("title", "첫날 저녁?", "multipleChoice", false, "anonymous", false,
                        "closesAt", "2027-01-01T00:00:00Z", "options", List.of("흑돼지", "해산물")));
        long pollId = poll.get("id").asLong();
        long optA = poll.get("options").get(0).get("id").asLong();
        long optB = poll.get("options").get(1).get("id").asLong();

        // viewer can vote (participation is allowed for viewers)
        mockMvc.perform(post("/api/polls/" + pollId + "/vote").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(optA)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVoters").value(1))
                .andExpect(jsonPath("$.myOptionIds[0]").value((int) optA));

        // changing the vote moves the count, doesn't add a second
        mockMvc.perform(post("/api/polls/" + pollId + "/vote").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(optB)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVoters").value(1))
                .andExpect(jsonPath("$.myOptionIds[0]").value((int) optB));

        // single-choice rejects multiple options
        mockMvc.perform(post("/api/polls/" + pollId + "/vote").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(optA, optB)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P002"));

        // viewers cannot create polls
        mockMvc.perform(post("/api/trips/" + tripId + "/polls").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "x", "closesAt",
                                "2027-01-01T00:00:00Z", "options", List.of("a", "b")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void closedPoll_rejectsVote() throws Exception {
        String owner = signupAndLogin("v_owner2@voyage.com");
        long tripId = createTrip(owner);
        JsonNode poll = createPoll(owner, tripId,
                Map.of("title", "지난 투표", "closesAt", "2020-01-01T00:00:00Z",
                        "options", List.of("a", "b")));
        long pollId = poll.get("id").asLong();
        long optA = poll.get("options").get(0).get("id").asLong();

        mockMvc.perform(post("/api/polls/" + pollId + "/vote").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(optA)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void anonymousPoll_hidesVoterIds() throws Exception {
        String owner = signupAndLogin("v_owner3@voyage.com");
        long tripId = createTrip(owner);
        JsonNode poll = createPoll(owner, tripId,
                Map.of("title", "익명 투표", "anonymous", true, "closesAt", "2027-01-01T00:00:00Z",
                        "options", List.of("a", "b")));
        long pollId = poll.get("id").asLong();
        long optA = poll.get("options").get(0).get("id").asLong();

        mockMvc.perform(post("/api/polls/" + pollId + "/vote").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(optA)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/polls/" + pollId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].voteCount").value(1))
                .andExpect(jsonPath("$.options[0].voterIds").doesNotExist());
    }

    private JsonNode createPoll(String token, long tripId, Map<String, Object> payload) throws Exception {
        String body = mockMvc.perform(post("/api/trips/" + tripId + "/polls")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
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
