package io.nextskip.activations.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.common.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActivationsServiceImpl.
 *
 * Tests the service layer that reads POTA and SOTA activations from the LoadingCache.
 */
@ExtendWith(MockitoExtension.class)
class ActivationsServiceImplTest {

    private static final String TEST_CALLSIGN_POTA = "W1ABC";
    private static final String TEST_CALLSIGN_SOTA = "W3XYZ/P";

    @Mock
    private LoadingCache<String, List<Activation>> activationsCache;

    private ActivationsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivationsServiceImpl(activationsCache);
    }

    @Test
    void shouldCombine_PotaAndSotaActivations() {
        // Given: Cache returns combined POTA and SOTA activations
        List<Activation> allActivations = List.of(
                createPotaActivation("1", TEST_CALLSIGN_POTA),
                createPotaActivation("2", "K2DEF"),
                createSotaActivation("3", TEST_CALLSIGN_SOTA)
        );

        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(allActivations);

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then
        assertNotNull(summary);
        assertEquals(3, summary.activations().size(), "Should combine all activations");
        assertEquals(2, summary.potaCount(), "Should count POTA activations");
        assertEquals(1, summary.sotaCount(), "Should count SOTA activations");
        assertNotNull(summary.lastUpdated(), "Should set timestamp");

        // Verify cache was called
        verify(activationsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldHandle_EmptyCache() {
        // Given: Cache returns empty list
        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then
        assertNotNull(summary);
        assertEquals(0, summary.activations().size());
        assertEquals(0, summary.potaCount());
        assertEquals(0, summary.sotaCount());
        assertNotNull(summary.lastUpdated());
    }

    @Test
    void shouldHandle_NullFromCache() {
        // Given: Cache returns null (edge case)
        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then: Should handle null gracefully
        assertNotNull(summary);
        assertEquals(0, summary.activations().size(), "Should have no activations");
        assertEquals(0, summary.potaCount(), "POTA count should be 0");
        assertEquals(0, summary.sotaCount(), "SOTA count should be 0");
        assertNotNull(summary.lastUpdated(), "Should still set timestamp");
    }

    @Test
    void shouldSet_RecentLastUpdatedTimestamp() {
        // Given
        Instant before = Instant.now();
        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        ActivationsSummary summary = service.getActivationsSummary();
        Instant after = Instant.now();

        // Then: Timestamp should be between before and after
        assertNotNull(summary.lastUpdated());
        assertTrue(
                summary.lastUpdated().equals(before) || summary.lastUpdated().isAfter(before),
                "Timestamp should be at or after start time"
        );
        assertTrue(
                summary.lastUpdated().equals(after) || summary.lastUpdated().isBefore(after),
                "Timestamp should be at or before end time"
        );
    }

    // ========== getActivationsResponse() tests ==========

    @Test
    void testGetActivationsResponse_SeparatesActivationsByType() {
        // Given: Mixed POTA and SOTA activations from cache
        List<Activation> allActivations = List.of(
                createPotaActivation("1", TEST_CALLSIGN_POTA),
                createPotaActivation("2", "K2DEF"),
                createSotaActivation("3", TEST_CALLSIGN_SOTA)
        );

        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(allActivations);

        // When
        io.nextskip.activations.api.ActivationsResponse response = service.getActivationsResponse();

        // Then: Should separate by type
        assertNotNull(response);
        assertEquals(2, response.potaActivations().size(), "Should have 2 POTA activations");
        assertEquals(1, response.sotaActivations().size(), "Should have 1 SOTA activation");

        // Verify types are correctly separated
        assertTrue(response.potaActivations().stream()
                        .allMatch(a -> a.type() == ActivationType.POTA),
                "potaActivations should only contain POTA type");
        assertTrue(response.sotaActivations().stream()
                        .allMatch(a -> a.type() == ActivationType.SOTA),
                "sotaActivations should only contain SOTA type");
    }

    @Test
    void testGetActivationsResponse_CalculatesTotalCount() {
        // Given
        List<Activation> allActivations = List.of(
                createPotaActivation("1", TEST_CALLSIGN_POTA),
                createPotaActivation("2", "K2DEF"),
                createPotaActivation("3", "N3GHI"),
                createSotaActivation("4", "W4XYZ/P"),
                createSotaActivation("5", "K5LMN/P")
        );

        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(allActivations);

        // When
        io.nextskip.activations.api.ActivationsResponse response = service.getActivationsResponse();

        // Then
        assertEquals(5, response.totalCount(), "Total count should be sum of POTA and SOTA");
        assertEquals(response.potaActivations().size() + response.sotaActivations().size(),
                response.totalCount(),
                "Total should equal sum of both lists");
    }

    @Test
    void testGetActivationsResponse_IncludesLastUpdated() {
        // Given
        Instant before = Instant.now();
        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        io.nextskip.activations.api.ActivationsResponse response = service.getActivationsResponse();
        Instant after = Instant.now();

        // Then
        assertNotNull(response.lastUpdated(), "Response should include lastUpdated timestamp");
        assertTrue(
                !response.lastUpdated().isBefore(before),
                "lastUpdated should be at or after test start"
        );
        assertTrue(
                !response.lastUpdated().isAfter(after),
                "lastUpdated should be at or before test end"
        );
    }

    /**
     * Helper method to create a test POTA activation.
     */
    private Activation createPotaActivation(String id, String callsign) {
        io.nextskip.activations.model.Park park = new io.nextskip.activations.model.Park(
                "US-0001",
                "Test Park",
                "CO",
                "US",
                "FN42",
                42.5,
                -71.3
        );

        return new Activation(
                id,
                callsign,
                ActivationType.POTA,
                14250.0,
                "SSB",
                Instant.now(),
                10,
                "POTA API",
                park
        );
    }

    /**
     * Helper method to create a test SOTA activation.
     */
    private Activation createSotaActivation(String id, String callsign) {
        io.nextskip.activations.model.Summit summit = new io.nextskip.activations.model.Summit(
                "W7W/LC-001",
                "Test Summit",
                "WA",
                "W7W"
        );

        return new Activation(
                id,
                callsign,
                ActivationType.SOTA,
                7200.0,
                "CW",
                Instant.now(),
                null,
                "SOTA API",
                summit
        );
    }
}
