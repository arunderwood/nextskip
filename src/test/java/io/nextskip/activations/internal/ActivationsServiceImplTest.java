package io.nextskip.activations.internal;

import static io.nextskip.test.fixtures.ActivationFixtures.pota;
import static io.nextskip.test.fixtures.ActivationFixtures.sota;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.common.config.CacheConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ActivationsServiceImpl.
 *
 * Tests the service layer that reads POTA and SOTA activations from the LoadingCache.
 */
@ExtendWith(MockitoExtension.class)
class ActivationsServiceImplTest {

    private static final String TEST_CALLSIGN_POTA = "W1ABC";
    private static final String TEST_CALLSIGN_SOTA = "W3XYZ/P";
    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    @Mock
    private LoadingCache<String, List<Activation>> activationsCache;

    private ActivationsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivationsServiceImpl(activationsCache, FIXED_CLOCK);
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
        // Given: Fixed clock is injected
        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then: Timestamp should match the fixed clock time exactly
        assertEquals(FIXED_TIME, summary.lastUpdated(), "Timestamp should match fixed clock time");
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
        // Given: Fixed clock is injected
        when(activationsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        io.nextskip.activations.api.ActivationsResponse response = service.getActivationsResponse();

        // Then: Timestamp should match the fixed clock time exactly
        assertEquals(FIXED_TIME, response.lastUpdated(), "lastUpdated should match fixed clock time");
    }

    /**
     * Helper method to create a test POTA activation.
     */
    private Activation createPotaActivation(String id, String callsign) {
        return pota().spotId(id).activatorCallsign(callsign).build();
    }

    /**
     * Helper method to create a test SOTA activation.
     */
    private Activation createSotaActivation(String id, String callsign) {
        return sota().spotId(id).activatorCallsign(callsign).build();
    }
}
