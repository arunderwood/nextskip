package io.nextskip.propagation.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PropagationServiceImpl.
 *
 * Tests the service layer that reads propagation data from LoadingCaches.
 */
@ExtendWith(MockitoExtension.class)
class PropagationServiceImplTest {

    private static final String MERGED_SOURCE = "NOAA SWPC + HamQSL";
    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    @Mock
    private LoadingCache<String, SolarIndices> solarIndicesCache;

    @Mock
    private LoadingCache<String, List<BandCondition>> bandConditionsCache;

    private PropagationServiceImpl service;

    private SolarIndices mergedSolarIndices;
    private List<BandCondition> bandConditions;

    @BeforeEach
    void setUp() {
        service = new PropagationServiceImpl(solarIndicesCache, bandConditionsCache, FIXED_CLOCK);

        // Setup test data - merged solar indices (as would be produced by cache loader)
        mergedSolarIndices = new SolarIndices(
                150.5,  // SFI from NOAA
                8,      // A-index from HamQSL
                3,      // K-index from HamQSL
                120,    // Sunspots from NOAA
                FIXED_TIME,
                MERGED_SOURCE
        );

        bandConditions = List.of(
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR),
                new BandCondition(FrequencyBand.BAND_15M, BandConditionRating.POOR)
        );
    }

    @Test
    void shouldGet_CurrentSolarIndices_FromCache() {
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(mergedSolarIndices);

        SolarIndices result = service.getCurrentSolarIndices();

        assertNotNull(result);
        assertEquals(150.5, result.solarFluxIndex(), 0.01);
        assertEquals(120, result.sunspotNumber());
        assertEquals(3, result.kIndex());
        assertEquals(8, result.aIndex());
        assertEquals(MERGED_SOURCE, result.source());

        verify(solarIndicesCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_CurrentSolarIndices_NullFromCache() {
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);

        SolarIndices result = service.getCurrentSolarIndices();

        assertNull(result);

        verify(solarIndicesCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandConditions_FromCache() {
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(bandConditions);

        List<BandCondition> result = service.getBandConditions();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(bandConditions, result);

        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandConditions_EmptyList() {
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        List<BandCondition> result = service.getBandConditions();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandConditions_NullFromCache() {
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);

        List<BandCondition> result = service.getBandConditions();

        // Should handle null gracefully and return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandCondition_Found() {
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(bandConditions);

        BandCondition result = service.getBandCondition(FrequencyBand.BAND_20M);

        assertNotNull(result);
        assertEquals(FrequencyBand.BAND_20M, result.band());
        assertEquals(BandConditionRating.GOOD, result.rating());

        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandCondition_NotFound() {
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(bandConditions);

        BandCondition result = service.getBandCondition(FrequencyBand.BAND_160M);

        assertNull(result);

        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandCondition_NullBand() {
        BandCondition result = service.getBandCondition(null);

        assertNull(result);

        verifyNoInteractions(bandConditionsCache);
    }

    @Test
    void shouldGet_SolarIndicesReactive() {
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(mergedSolarIndices);

        StepVerifier.create(service.getSolarIndicesReactive())
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(150.5, result.solarFluxIndex(), 0.01);
                    assertEquals(MERGED_SOURCE, result.source());
                })
                .verifyComplete();

        verify(solarIndicesCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void shouldGet_BandConditionsReactive() {
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(bandConditions);

        StepVerifier.create(service.getBandConditionsReactive())
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(3, result.size());
                })
                .verifyComplete();

        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    // ========== getPropagationResponse() tests ==========

    @Test
    void testGetPropagationResponse_CombinesSolarAndBandData() {
        // Given: Both solar indices and band conditions available from caches
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(mergedSolarIndices);
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(bandConditions);

        // When
        io.nextskip.propagation.api.PropagationResponse response = service.getPropagationResponse();

        // Then
        assertNotNull(response);
        assertNotNull(response.solarIndices(), "Response should include solar indices");
        assertNotNull(response.bandConditions(), "Response should include band conditions");

        // Verify solar data is included
        assertEquals(150.5, response.solarIndices().solarFluxIndex(), 0.01);
        assertEquals(MERGED_SOURCE, response.solarIndices().source());

        // Verify band conditions are included
        assertEquals(3, response.bandConditions().size());
        assertTrue(response.bandConditions().stream()
                .anyMatch(bc -> bc.band() == FrequencyBand.BAND_20M));

        verify(solarIndicesCache).get(CacheConfig.CACHE_KEY);
        verify(bandConditionsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void testGetPropagationResponse_HandlesNullSolarIndices() {
        // Given: Solar indices unavailable, band conditions available
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(bandConditions);

        // When
        io.nextskip.propagation.api.PropagationResponse response = service.getPropagationResponse();

        // Then: Should still return response with null solar indices
        assertNotNull(response);
        assertNull(response.solarIndices(), "Solar indices should be null when unavailable");
        assertNotNull(response.bandConditions(), "Band conditions should still be present");
        assertEquals(3, response.bandConditions().size());
    }

    @Test
    void testGetPropagationResponse_HandlesEmptyBandConditions() {
        // Given: Solar indices available, band conditions empty
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(mergedSolarIndices);
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        io.nextskip.propagation.api.PropagationResponse response = service.getPropagationResponse();

        // Then: Should return response with empty band conditions list
        assertNotNull(response);
        assertNotNull(response.solarIndices(), "Solar indices should be present");
        assertNotNull(response.bandConditions(), "Band conditions should not be null");
        assertTrue(response.bandConditions().isEmpty(), "Band conditions should be empty");
    }

    @Test
    void testGetPropagationResponse_HandlesNullBandConditionsFromCache() {
        // Given: Band cache returns null
        when(solarIndicesCache.get(CacheConfig.CACHE_KEY)).thenReturn(mergedSolarIndices);
        when(bandConditionsCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);

        // When
        io.nextskip.propagation.api.PropagationResponse response = service.getPropagationResponse();

        // Then: Should handle null gracefully
        assertNotNull(response);
        assertNotNull(response.solarIndices());
        // PropagationResponse's compact constructor converts null to empty list
        assertNotNull(response.bandConditions());
    }
}
