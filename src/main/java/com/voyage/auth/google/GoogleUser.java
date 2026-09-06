package com.voyage.auth.google;

/** Identity extracted from a verified Google ID token. */
public record GoogleUser(String providerId, String email, String name) {
}
