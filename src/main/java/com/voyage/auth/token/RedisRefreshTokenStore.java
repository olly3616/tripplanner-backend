package com.voyage.auth.token;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed refresh-token store ({@code voyage.auth.token-store=redis}).
 * Each token hash is a key with the owner's id and a TTL; expiry and revocation
 * are just key expiry/deletion. Rotation is a delete + set by the caller.
 */
@Component
@ConditionalOnProperty(name = "voyage.auth.token-store", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String tokenHash, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + tokenHash, String.valueOf(userId), ttl);
    }

    @Override
    public Optional<Long> findActiveUserId(String tokenHash) {
        String userId = redisTemplate.opsForValue().get(PREFIX + tokenHash);
        return userId == null ? Optional.empty() : Optional.of(Long.valueOf(userId));
    }

    @Override
    public void revoke(String tokenHash) {
        redisTemplate.delete(PREFIX + tokenHash);
    }
}
