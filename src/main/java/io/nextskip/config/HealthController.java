package io.nextskip.config;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health endpoint for Render load balancer checks.
 *
 * <p>Render health-checks `/actuator/health`, but actuator runs on port 9090 (not exposed).
 * Without this endpoint, health checks fall through to Vaadin's catch-all route, polluting
 * metrics. This provides a clean endpoint excluded from OTEL instrumentation.
 *
 * <p>Requires: {@code OTEL_INSTRUMENTATION_SPRING_WEBMVC_EXCLUDED_PATHS=/health}
 */
@RestController
public class HealthController {

    /**
     * Returns basic health status for load balancer checks.
     *
     * @return health status map
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
