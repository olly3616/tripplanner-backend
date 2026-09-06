package com.voyage.auth.token;

import com.voyage.auth.domain.RefreshToken;
import com.voyage.auth.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default refresh-token store backed by the {@code refresh_tokens} table.
 * Runs inside the caller's transaction (revocation is dirty-checked).
 */
@Component
@ConditionalOnProperty(name = "voyage.auth.token-store", havingValue = "db", matchIfMissing = true)
@RequiredArgsConstructor
public class DbRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void save(String tokenHash, Long userId, Duration ttl) {
        refreshTokenRepository.save(RefreshToken.issue(userId, tokenHash, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<Long> findActiveUserId(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> token.isActive(Instant.now()))
                .map(RefreshToken::getUserId);
    }

    @Override
    public void revoke(String tokenHash) {
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> token.revoke(Instant.now()));
    }
}
