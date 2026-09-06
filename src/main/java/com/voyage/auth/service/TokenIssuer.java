package com.voyage.auth.service;

import com.voyage.auth.dto.TokenResponse;
import com.voyage.auth.jwt.JwtProperties;
import com.voyage.auth.jwt.JwtTokenProvider;
import com.voyage.auth.token.RefreshTokenStore;
import com.voyage.global.util.SecureTokens;
import com.voyage.user.domain.User;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Issues an access + refresh token pair for a user. Shared by email/password
 * and social login so token issuance lives in exactly one place.
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;

    public TokenResponse issue(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = SecureTokens.newToken();
        refreshTokenStore.save(SecureTokens.sha256Hex(rawRefreshToken), user.getId(),
                Duration.ofSeconds(jwtProperties.refreshTokenTtlSeconds()));
        return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenTtlSeconds(), rawRefreshToken);
    }
}
