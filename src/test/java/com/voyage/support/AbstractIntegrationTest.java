package com.voyage.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests. Boots the full Spring context against a
 * real MySQL instance started via Testcontainers, so tests exercise the same
 * SQL dialect and Flyway migrations as production.
 *
 * <p>{@code disabledWithoutDocker = true} makes these tests skip automatically
 * on machines without Docker (e.g. some CI runners or local setups), instead of
 * failing the build.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
}
