package com.voyage.storage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class ReceiptIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadReceipt_storesAndSetsUrl_validatesType_andRole() throws Exception {
        String owner = signupAndLogin("r_owner@voyage.com");
        String viewer = signupAndLogin("r_viewer@voyage.com");
        long ownerId = meId(owner);
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "VIEWER", viewer);
        long expenseId = createExpense(owner, tripId, ownerId);

        MockMultipartFile png = new MockMultipartFile(
                "file", "receipt.png", "image/png", new byte[]{1, 2, 3, 4});

        // owner (editor) uploads a valid image -> receiptUrl set
        mockMvc.perform(multipart("/api/expenses/" + expenseId + "/receipt")
                        .file(png).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptUrl").exists());

        // disallowed content type -> 400 F001
        MockMultipartFile txt = new MockMultipartFile(
                "file", "note.txt", "text/plain", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/expenses/" + expenseId + "/receipt")
                        .file(txt).header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));

        // viewer cannot upload -> 403
        mockMvc.perform(multipart("/api/expenses/" + expenseId + "/receipt")
                        .file(png).header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
    }

    private long createExpense(String token, long tripId, long payerId) throws Exception {
        String body = mockMvc.perform(post("/api/trips/" + tripId + "/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "택시", "amountMinor", 30_000L, "currency", "KRW",
                                "payerId", payerId, "splitMethod", "EQUAL", "spentOn", "2026-08-14",
                                "participants", List.of(Map.of("userId", payerId))))))
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

    private long meId(String token) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/users/me").header("Authorization", "Bearer " + token))
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
