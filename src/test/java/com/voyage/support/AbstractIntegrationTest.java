package com.voyage.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests. Boots the full Spring context against a
 * real MySQL instance started via Testcontainers, so tests exercise the same
 * SQL dialect and Flyway migrations as production.
 *
 * <p><b>Singleton container:</b> the MySQL container is started once per JVM in
 * a static initializer and never stopped (Ryuk cleans it up at JVM exit). This
 * keeps a stable host/port across every integration test class, so Spring's
 * cached application context — bound via {@link ServiceConnection} — never ends
 * up pointing at a stopped container. (Using {@code @Container}, which stops the
 * container after each class, breaks the second class once contexts are cached.)
 *
 * <p>{@code disabledWithoutDocker = true} skips these tests when Docker is
 * unavailable (the static initializer only runs once tests actually execute).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    public static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}
