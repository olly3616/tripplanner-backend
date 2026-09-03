package com.voyage.auth.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates cryptographically strong, opaque refresh tokens.
 * The raw value is returned to the client; only its hash is persisted.
 */
public final class RefreshTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private RefreshTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
