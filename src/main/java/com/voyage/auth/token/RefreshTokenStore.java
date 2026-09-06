package com.voyage.auth.token;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage for refresh tokens (only the token hash is kept). Two adapters:
 * DB (default) and Redis ({@code voyage.auth.token-store=redis}).
 */
public interface RefreshTokenStore {

    void save(String tokenHash, Long userId, Duration ttl);

    /** Returns the owning user id if the token is present and still valid. */
    Optional<Long> findActiveUserId(String tokenHash);

    void revoke(String tokenHash);
}
