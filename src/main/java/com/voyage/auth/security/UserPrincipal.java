package com.voyage.auth.security;

/**
 * Authenticated principal placed in the SecurityContext by
 * {@link JwtAuthenticationFilter}. Accessible in controllers via
 * {@code @AuthenticationPrincipal UserPrincipal}.
 */
public record UserPrincipal(Long id, String email) {
}
