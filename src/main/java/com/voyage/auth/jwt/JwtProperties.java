package com.voyage.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration bound from {@code voyage.jwt.*}.
 *
 * @param secret                  HS256 signing secret (>= 32 bytes)
 * @param accessTokenTtlSeconds   access token lifetime in seconds
 * @param refreshTokenTtlSeconds  refresh token lifetime in seconds
 */
@ConfigurationProperties(prefix = "voyage.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds
) {
}
