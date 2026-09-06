package com.voyage.auth.google;

/**
 * Verifies a Google ID token and returns the identity. Implementations throw a
 * BusinessException(INVALID_TOKEN) if the token is invalid. The real adapter is
 * active only when a Google client id is configured.
 */
@FunctionalInterface
public interface GoogleTokenVerifier {

    GoogleUser verify(String idToken);
}
