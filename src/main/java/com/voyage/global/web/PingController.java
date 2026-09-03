package com.voyage.global.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight liveness endpoint for smoke tests and uptime checks.
 * Kept public in {@link com.voyage.global.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "service", "voyage-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
