package io.nextskip.common.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CacheConfig.
 *
 * <p>Verifies that all LoadingCache beans are properly configured
 * and can load data from their respective repositories. Also tests
 * the extracted loader methods directly for coverage.
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    private static final String SOURCE_NOAA = "noaa";
    private static final String SOURCE_HAMQSL = "hamqsl";

    @Mock
    private ActivationRepository activationRepository;

    @Mock
    private SolarIndicesRepository solarIndicesRepository;

    @Mock
    private BandConditionRepository bandConditionRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private MeteorShowerRepository meteorShowerRepository;

    private CacheConfig config;

    @BeforeEach
    void setUp() {
        config = new CacheConfig();
    }

    @Test
    void testCacheKey_IsCorrectValue() {
        assertEquals("all", CacheConfig.CACHE_KEY, "Cache key should be 'all'");
    }

    @Test
    void testActivationsCache_LoadsFromRepository() {
        // Given: Repository returns empty list
        when(activationRepository.findBySpottedAtAfterOrderBySpottedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // When: Create cache and get data
        LoadingCache<String, List<Activation>> cache = config.activationsCache(activationRepository);

        // Then: Cache should be created and load from repository
        assertNotNull(cache);
        List<Activation> result = cache.get(CacheConfig.CACHE_KEY);
        assertNotNull(result);
    }

    @Test
    void testSolarIndicesCache_LoadsFromRepository() {
        // Given: Repository returns empty optionals
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_NOAA))
                .thenReturn(Optional.empty());
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_HAMQSL))
                .thenReturn(Optional.empty());

        // When: Create cache and get data
        LoadingCache<String, SolarIndices> cache = config.solarIndicesCache(solarIndicesRepository);

        // Then: Cache should be created (result may be null if no data)
        assertNotNull(cache);
        // Note: get() may return null when no data sources available
        cache.get(CacheConfig.CACHE_KEY);
    }

    @Test
    void testBandConditionsCache_LoadsFromRepository() {
        // Given: Repository returns empty list
        when(bandConditionRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // When: Create cache and get data
        LoadingCache<String, List<BandCondition>> cache = config.bandConditionsCache(bandConditionRepository);

        // Then: Cache should be created and load from repository
        assertNotNull(cache);
        List<BandCondition> result = cache.get(CacheConfig.CACHE_KEY);
        assertNotNull(result);
    }

    @Test
    void testContestsCache_LoadsFromRepository() {
        // Given: Repository returns empty list
        when(contestRepository.findByEndTimeAfterOrderByStartTimeAsc(any()))
                .thenReturn(Collections.emptyList());

        // When: Create cache and get data
        LoadingCache<String, List<Contest>> cache = config.contestsCache(contestRepository);

        // Then: Cache should be created and load from repository
        assertNotNull(cache);
        List<Contest> result = cache.get(CacheConfig.CACHE_KEY);
        assertNotNull(result);
    }

    @Test
    void testMeteorShowersCache_LoadsFromRepository() {
        // Given: Repository returns empty list
        when(meteorShowerRepository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(any(), any()))
                .thenReturn(Collections.emptyList());

        // When: Create cache and get data
        LoadingCache<String, List<MeteorShower>> cache = config.meteorShowersCache(meteorShowerRepository);

        // Then: Cache should be created and load from repository
        assertNotNull(cache);
        List<MeteorShower> result = cache.get(CacheConfig.CACHE_KEY);
        assertNotNull(result);
    }

    // ========== Loader method tests (direct invocation for coverage) ==========

    @Test
    void testLoadActivations_ReturnsActivationsFromRepository() {
        // Given: Repository returns activation entities
        Instant now = Instant.now();
        Activation activation = new Activation(
                "spot-1", "W1ABC", ActivationType.POTA, 14250.0, "SSB", now, 5, "pota",
                new Park("K-1234", "Test Park", "CA", "US", "CM97", 37.5, -122.1)
        );
        ActivationEntity entity = ActivationEntity.fromDomain(activation);
        when(activationRepository.findBySpottedAtAfterOrderBySpottedAtDesc(any()))
                .thenReturn(List.of(entity));

        // When: Call loader method directly
        List<Activation> result = config.loadActivations(activationRepository);

        // Then: Should return mapped domain objects
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("W1ABC", result.getFirst().activatorCallsign());
        assertEquals(ActivationType.POTA, result.getFirst().type());
    }

    @Test
    void testLoadActivations_ReturnsEmptyList() {
        // Given: Repository returns empty list
        when(activationRepository.findBySpottedAtAfterOrderBySpottedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // When: Call loader method directly
        List<Activation> result = config.loadActivations(activationRepository);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadAndMergeSolarIndices_MergesBothSources() {
        // Given: Both NOAA and HamQSL data available
        Instant now = Instant.now();
        SolarIndicesEntity noaaEntity = new SolarIndicesEntity(150.0, 5, 2, 120, now, SOURCE_NOAA);
        SolarIndicesEntity hamqslEntity = new SolarIndicesEntity(145.0, 8, 3, 115, now, SOURCE_HAMQSL);
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_NOAA))
                .thenReturn(Optional.of(noaaEntity));
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_HAMQSL))
                .thenReturn(Optional.of(hamqslEntity));

        // When: Call loader method directly
        SolarIndices result = config.loadAndMergeSolarIndices(solarIndicesRepository);

        // Then: Should merge SFI/sunspots from NOAA, K/A from HamQSL
        assertNotNull(result);
        assertEquals(150.0, result.solarFluxIndex(), 0.01); // NOAA SFI
        assertEquals(120, result.sunspotNumber()); // NOAA sunspots
        assertEquals(8, result.aIndex()); // HamQSL A-index
        assertEquals(3, result.kIndex()); // HamQSL K-index
        assertEquals("NOAA SWPC + HamQSL", result.source());
    }

    @Test
    void testLoadAndMergeSolarIndices_NoaaOnly() {
        // Given: Only NOAA data available
        Instant now = Instant.now();
        SolarIndicesEntity noaaEntity = new SolarIndicesEntity(150.0, 5, 2, 120, now, SOURCE_NOAA);
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_NOAA))
                .thenReturn(Optional.of(noaaEntity));
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_HAMQSL))
                .thenReturn(Optional.empty());

        // When: Call loader method directly
        SolarIndices result = config.loadAndMergeSolarIndices(solarIndicesRepository);

        // Then: Should return NOAA data only
        assertNotNull(result);
        assertEquals(150.0, result.solarFluxIndex(), 0.01);
        assertEquals(SOURCE_NOAA, result.source());
    }

    @Test
    void testLoadAndMergeSolarIndices_HamqslOnly() {
        // Given: Only HamQSL data available
        Instant now = Instant.now();
        SolarIndicesEntity hamqslEntity = new SolarIndicesEntity(145.0, 8, 3, 115, now, SOURCE_HAMQSL);
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_NOAA))
                .thenReturn(Optional.empty());
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_HAMQSL))
                .thenReturn(Optional.of(hamqslEntity));

        // When: Call loader method directly
        SolarIndices result = config.loadAndMergeSolarIndices(solarIndicesRepository);

        // Then: Should return HamQSL data only
        assertNotNull(result);
        assertEquals(145.0, result.solarFluxIndex(), 0.01);
        assertEquals(SOURCE_HAMQSL, result.source());
    }

    @Test
    void testLoadAndMergeSolarIndices_NoDataReturnsNull() {
        // Given: No data available from either source
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_NOAA))
                .thenReturn(Optional.empty());
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc(SOURCE_HAMQSL))
                .thenReturn(Optional.empty());

        // When: Call loader method directly
        SolarIndices result = config.loadAndMergeSolarIndices(solarIndicesRepository);

        // Then: Should return null
        assertNull(result);
    }

    @Test
    void testLoadBandConditions_ReturnsBandConditionsFromRepository() {
        // Given: Repository returns band condition entities
        BandConditionEntity entity = new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.9, "Excellent", Instant.now()
        );
        when(bandConditionRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any()))
                .thenReturn(List.of(entity));

        // When: Call loader method directly
        List<BandCondition> result = config.loadBandConditions(bandConditionRepository);

        // Then: Should return mapped domain objects
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(FrequencyBand.BAND_20M, result.getFirst().band());
        assertEquals(BandConditionRating.GOOD, result.getFirst().rating());
    }

    @Test
    void testLoadBandConditions_ReturnsEmptyList() {
        // Given: Repository returns empty list
        when(bandConditionRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // When: Call loader method directly
        List<BandCondition> result = config.loadBandConditions(bandConditionRepository);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadContests_ReturnsContestsFromRepository() {
        // Given: Repository returns contest entities
        Instant start = Instant.now();
        Instant end = start.plusSeconds(86400);
        ContestEntity entity = new ContestEntity(
                "Test Contest", start, end, Set.of(FrequencyBand.BAND_20M),
                Set.of("CW", "SSB"), "ARRL", null, null
        );
        when(contestRepository.findByEndTimeAfterOrderByStartTimeAsc(any()))
                .thenReturn(List.of(entity));

        // When: Call loader method directly
        List<Contest> result = config.loadContests(contestRepository);

        // Then: Should return mapped domain objects
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Contest", result.getFirst().name());
        assertTrue(result.getFirst().bands().contains(FrequencyBand.BAND_20M));
    }

    @Test
    void testLoadContests_ReturnsEmptyList() {
        // Given: Repository returns empty list
        when(contestRepository.findByEndTimeAfterOrderByStartTimeAsc(any()))
                .thenReturn(Collections.emptyList());

        // When: Call loader method directly
        List<Contest> result = config.loadContests(contestRepository);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadMeteorShowers_ReturnsMeteorShowersFromRepository() {
        // Given: Repository returns meteor shower entities
        Instant now = Instant.now();
        MeteorShowerEntity entity = new MeteorShowerEntity(
                "Perseids 2025", "PER", now, now.plusSeconds(86400),
                now.minusSeconds(604800), now.plusSeconds(604800), 100,
                "109P/Swift-Tuttle", "https://ams.org/perseids"
        );
        when(meteorShowerRepository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(any(), any()))
                .thenReturn(List.of(entity));

        // When: Call loader method directly
        List<MeteorShower> result = config.loadMeteorShowers(meteorShowerRepository);

        // Then: Should return mapped domain objects
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Perseids 2025", result.getFirst().name());
        assertEquals("PER", result.getFirst().code());
        assertEquals(100, result.getFirst().peakZhr());
    }

    @Test
    void testLoadMeteorShowers_ReturnsEmptyList() {
        // Given: Repository returns empty list
        when(meteorShowerRepository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(any(), any()))
                .thenReturn(Collections.emptyList());

        // When: Call loader method directly
        List<MeteorShower> result = config.loadMeteorShowers(meteorShowerRepository);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
