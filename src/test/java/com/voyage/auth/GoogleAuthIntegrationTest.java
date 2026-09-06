package com.voyage.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voyage.auth.google.GoogleTokenVerifier;
import com.voyage.auth.google.GoogleUser;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@Import(GoogleAuthIntegrationTest.StubVerifierConfig.class)
class GoogleAuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class StubVerifierConfig {
        @Bean
        GoogleTokenVerifier stubVerifier() {
            return idToken -> {
                if ("valid-token".equals(idToken)) {
                    return new GoogleUser("g-123", "gtest@voyage.com", "Google Tester");
                }
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            };
        }
    }

    @Test
    void googleLogin_createsUserThenLogsInSameUser() throws Exception {
        String first = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "valid-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(first).get("accessToken").asString();

        // the issued token identifies the Google user
        String me = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode meNode = objectMapper.readTree(me);
        long firstUserId = meNode.get("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals("gtest@voyage.com", meNode.get("email").asString());

        // logging in again resolves the same user (no duplicate)
        String second = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "valid-token"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondAccess = objectMapper.readTree(second).get("accessToken").asString();
        long secondUserId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + secondAccess))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(firstUserId, secondUserId);
    }

    @Test
    void invalidGoogleToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "bad-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A003"));
    }
}
