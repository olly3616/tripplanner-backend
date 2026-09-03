package com.voyage;

import com.voyage.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the full application context boots and Flyway migrations apply
 * against a real MySQL. Skipped automatically when Docker is unavailable.
 */
class VoyageBackendApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
