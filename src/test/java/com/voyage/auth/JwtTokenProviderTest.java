package com.voyage.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voyage.auth.jwt.JwtProperties;
import com.voyage.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdef-0123456789-xyz";

    @Test
    void createAccessToken_roundTripsUserIdAndEmail() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 900, 1_209_600));

        String token = provider.createAccessToken(42L, "minji@voyage.com");

        assertTrue(provider.isValid(token));
        assertEquals(42L, provider.getUserId(token));
        assertEquals("minji@voyage.com", provider.getEmail(token));
        assertEquals(900L, provider.getAccessTokenTtlSeconds());
    }

    @Test
    void expiredToken_isInvalid() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, -1, 1_209_600));

        String token = provider.createAccessToken(1L, "minji@voyage.com");

        assertFalse(provider.isValid(token));
    }

    @Test
    void tamperedToken_isInvalid() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 900, 1_209_600));

        String token = provider.createAccessToken(1L, "minji@voyage.com");

        assertFalse(provider.isValid(token + "tampered"));
    }
}
