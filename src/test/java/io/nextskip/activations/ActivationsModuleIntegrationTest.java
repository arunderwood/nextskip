package io.nextskip.activations;

import io.nextskip.activations.api.ActivationsEndpoint;
import io.nextskip.activations.api.ActivationsResponse;
import io.nextskip.activations.api.ActivationsService;
import io.nextskip.activations.internal.ActivationsServiceImpl;
import io.nextskip.activations.internal.PotaClient;
import io.nextskip.activations.internal.SotaClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Activations module.
 *
 * Tests Spring Boot context loading and bean wiring.
 * Tests with real API clients to verify the full integration stack.
 */
@SpringBootTest
@ActiveProfiles("test")
class ActivationsModuleIntegrationTest {

    @Autowired
    private ActivationsEndpoint endpoint;

    @Autowired
    private ActivationsService service;

    @Autowired
    private PotaClient potaClient;

    @Autowired
    private SotaClient sotaClient;

    @Test
    void contextLoads() {
        // Verify all required beans are loaded and autowired correctly
        assertNotNull(endpoint, "ActivationsEndpoint should be autowired");
        assertNotNull(service, "ActivationsService should be autowired");
        assertNotNull(potaClient, "PotaClient should be autowired");
        assertNotNull(sotaClient, "SotaClient should be autowired");
    }

    @Test
    void serviceImplementationIsCorrectType() {
        // Verify the service implementation is the expected class
        assertTrue(service instanceof ActivationsServiceImpl,
                "Service should be instance of ActivationsServiceImpl");
    }

    @Test
    void endpointReturnsActivationsResponse() {
        // When: Call endpoint (may hit real APIs or cache)
        ActivationsResponse response = endpoint.getActivations();

        // Then: Verify response structure is valid
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.potaActivations(), "POTA activations list should not be null");
        assertNotNull(response.sotaActivations(), "SOTA activations list should not be null");
        assertTrue(response.totalCount() >= 0, "Total count should be non-negative");
        assertNotNull(response.lastUpdated(), "Last updated timestamp should be set");
    }

    @Test
    void endpointTotalCountMatchesActivations() {
        // When: Call endpoint
        ActivationsResponse response = endpoint.getActivations();

        // Then: Total count should equal sum of POTA and SOTA
        int expectedTotal = response.potaActivations().size() + response.sotaActivations().size();
        assertEquals(expectedTotal, response.totalCount(),
                "Total count should match sum of POTA and SOTA activations");
    }

    @Test
    void responseTimestampIsRecent() {
        // When: Call endpoint
        ActivationsResponse response = endpoint.getActivations();

        // Then: Last updated timestamp should be recent (within last minute)
        var now = java.time.Instant.now();
        var lastUpdated = response.lastUpdated();
        var secondsAgo = now.getEpochSecond() - lastUpdated.getEpochSecond();

        assertTrue(secondsAgo >= 0, "Last updated should not be in the future");
        assertTrue(secondsAgo < 60, "Last updated should be within last minute");
    }
}
