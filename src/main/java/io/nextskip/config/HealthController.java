package io.nextskip.config;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health endpoint for Render load balancer checks.
 *
 * <p>Render can only health-check the main exposed port (8080). Actuator runs on port 9090
 * for security, so this provides a simple liveness check on the public port.
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
