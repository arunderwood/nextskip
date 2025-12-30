package io.nextskip.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for HealthController.
 */
class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void testHealth_ReturnsUpStatus() {
        Map<String, String> result = controller.health();

        assertNotNull(result);
        assertEquals("UP", result.get("status"));
        assertEquals(1, result.size());
    }
}
