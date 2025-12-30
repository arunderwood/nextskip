package io.nextskip.common.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CacheConfig.
 *
 * <p>Verifies that all LoadingCache beans are properly configured
 * and can load data from their respective repositories.
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

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
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc("noaa"))
                .thenReturn(Optional.empty());
        when(solarIndicesRepository.findTopBySourceOrderByTimestampDesc("hamqsl"))
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
}
