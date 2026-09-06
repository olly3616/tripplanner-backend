package com.voyage.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voyage.auth.token.RedisRefreshTokenStore;
import com.voyage.auth.token.RefreshTokenStore;
import com.voyage.support.AbstractIntegrationTest;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises the Redis-backed refresh-token store against a real Redis container.
 * Reuses the shared MySQL (the JPA context still needs a datasource) and flips
 * {@code voyage.auth.token-store=redis} so the Redis adapter is the active bean.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RedisRefreshTokenStoreTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = AbstractIntegrationTest.MYSQL;

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("voyage.auth.token-store", () -> "redis");
    }

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Test
    void redisAdapterIsActive() {
        assertInstanceOf(RedisRefreshTokenStore.class, refreshTokenStore);
    }

    @Test
    void saveFindRevoke() {
        refreshTokenStore.save("hash-1", 42L, Duration.ofMinutes(5));
        assertEquals(Optional.of(42L), refreshTokenStore.findActiveUserId("hash-1"));

        refreshTokenStore.revoke("hash-1");
        assertTrue(refreshTokenStore.findActiveUserId("hash-1").isEmpty());
    }

    @Test
    void unknownToken_isEmpty() {
        assertTrue(refreshTokenStore.findActiveUserId("does-not-exist").isEmpty());
    }
}
