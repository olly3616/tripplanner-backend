package com.voyage.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voyage.support.AbstractIntegrationTest;
import java.util.HashMap;
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
class ExpenseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void threePeople_settlementSumsToZero() throws Exception {
        String owner = signupAndLogin("e_owner@voyage.com");
        String m1 = signupAndLogin("e_m1@voyage.com");
        String m2 = signupAndLogin("e_m2@voyage.com");
        long ownerId = meId(owner);
        long id1 = meId(m1);
        long id2 = meId(m2);
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "EDITOR", m1);
        inviteAndAccept(owner, tripId, "EDITOR", m2);

        List<Long> all = List.of(ownerId, id1, id2);
        createEqualExpense(owner, tripId, "숙소비", 240_000L, ownerId, all);
        createEqualExpense(m1, tripId, "저녁", 60_000L, id1, all);
        createEqualExpense(m2, tripId, "택시", 30_000L, id2, all);

        String body = mockMvc.perform(get("/api/trips/" + tripId + "/settlement")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("KRW"))
                .andExpect(jsonPath("$.totalBaseMinor").value(330_000))
                .andReturn().getResponse().getContentAsString();

        JsonNode settlement = objectMapper.readTree(body);
        Map<Long, Long> net = new HashMap<>();
        long sum = 0;
        for (JsonNode b : settlement.get("balances")) {
            long value = b.get("netMinor").asLong();
            net.put(b.get("userId").asLong(), value);
            sum += value;
        }
        assertEquals(0, sum, "all net balances must sum to zero");
        assertEquals(130_000L, net.get(ownerId));   // paid 240k, owes 110k
        assertEquals(-50_000L, net.get(id1));        // paid 60k, owes 110k
        assertEquals(-80_000L, net.get(id2));        // paid 30k, owes 110k

        // Recommended transfers zero everyone out.
        Map<Long, Long> applied = new HashMap<>(net);
        for (JsonNode t : settlement.get("transfers")) {
            long amt = t.get("amountMinor").asLong();
            applied.merge(t.get("fromUserId").asLong(), amt, Long::sum);
            applied.merge(t.get("toUserId").asLong(), -amt, Long::sum);
        }
        assertEquals(true, applied.values().stream().allMatch(v -> v == 0));
    }

    @Test
    void multiCurrencyExpense_snapshotsBaseAmount() throws Exception {
        String owner = signupAndLogin("e_owner2@voyage.com");
        long ownerId = meId(owner);
        long tripId = createTrip(owner);

        // ¥10,000 with stub rate JPY->KRW 9.5 => 95,000 KRW
        mockMvc.perform(post("/api/trips/" + tripId + "/expenses").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expensePayload(
                                "라멘", 10_000L, "JPY", "EQUAL", ownerId, List.of(Map.of("userId", ownerId))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseAmountMinor").value(95_000))
                .andExpect(jsonPath("$.exchangeRate").value(9.5));
    }

    @Test
    void viewerCannotCreate_andExactMismatchIsRejected() throws Exception {
        String owner = signupAndLogin("e_owner3@voyage.com");
        String viewer = signupAndLogin("e_viewer3@voyage.com");
        long ownerId = meId(owner);
        long tripId = createTrip(owner);
        inviteAndAccept(owner, tripId, "VIEWER", viewer);

        mockMvc.perform(post("/api/trips/" + tripId + "/expenses").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expensePayload(
                                "몰래", 1_000L, "KRW", "EQUAL", ownerId, List.of(Map.of("userId", ownerId))))))
                .andExpect(status().isForbidden());

        // EXACT split whose parts don't sum to the amount -> 400 E001
        Map<String, Object> payload = expensePayload("점심", 10_000L, "KRW", "EXACT", ownerId, List.of(
                Map.of("userId", ownerId, "amountMinor", 4_000L)));
        mockMvc.perform(post("/api/trips/" + tripId + "/expenses").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"));
    }

    private void createEqualExpense(String token, long tripId, String title, long amount,
                                    long payerId, List<Long> participantIds) throws Exception {
        List<Map<String, Object>> participants = participantIds.stream()
                .map(id -> Map.<String, Object>of("userId", id)).toList();
        mockMvc.perform(post("/api/trips/" + tripId + "/expenses").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                expensePayload(title, amount, "KRW", "EQUAL", payerId, participants))))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> expensePayload(String title, long amount, String currency, String method,
                                               long payerId, List<Map<String, Object>> participants) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("amountMinor", amount);
        payload.put("currency", currency);
        payload.put("payerId", payerId);
        payload.put("splitMethod", method);
        payload.put("spentOn", "2026-08-14");
        payload.put("participants", participants);
        return payload;
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
