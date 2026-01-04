package io.nextskip.meteors.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.meteors.model.MeteorShower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MeteorServiceImpl.
 *
 * Tests the service layer that reads meteor shower data from the LoadingCache.
 */
@ExtendWith(MockitoExtension.class)
class MeteorServiceImplTest {

    private static final String ACTIVE_SHOWER = "Active";

    @Mock
    private LoadingCache<String, List<MeteorShower>> meteorShowersCache;

    private MeteorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MeteorServiceImpl(meteorShowersCache);
    }

    @Test
    void getMeteorShowers_filtersOutEnded() {
        Instant now = Instant.now();
        MeteorShower active = createShower(ACTIVE_SHOWER, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        MeteorShower ended = createShower("Ended", now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(5)));

        when(meteorShowersCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of(active, ended));

        List<MeteorShower> result = service.getMeteorShowers();

        assertEquals(1, result.size());
        assertEquals(ACTIVE_SHOWER, result.get(0).name());

        verify(meteorShowersCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void getMeteorShowers_emptyCache() {
        when(meteorShowersCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        List<MeteorShower> result = service.getMeteorShowers();

        assertTrue(result.isEmpty());

        verify(meteorShowersCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void getMeteorShowers_nullFromCache() {
        when(meteorShowersCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);

        List<MeteorShower> result = service.getMeteorShowers();

        // Should handle null gracefully
        assertTrue(result.isEmpty());

        verify(meteorShowersCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void getActiveShowers_returnsOnlyActive() {
        Instant now = Instant.now();
        MeteorShower active = createShower(ACTIVE_SHOWER, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        MeteorShower upcoming = createShower("Upcoming", now.plus(Duration.ofDays(5)), now.plus(Duration.ofDays(10)));

        when(meteorShowersCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of(active, upcoming));

        List<MeteorShower> result = service.getActiveShowers();

        assertEquals(1, result.size());
        assertEquals(ACTIVE_SHOWER, result.get(0).name());

        verify(meteorShowersCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void getUpcomingShowers_returnsOnlyUpcoming() {
        Instant now = Instant.now();
        MeteorShower active = createShower(ACTIVE_SHOWER, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        MeteorShower upcoming = createShower("Upcoming", now.plus(Duration.ofDays(5)), now.plus(Duration.ofDays(10)));

        when(meteorShowersCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of(active, upcoming));

        List<MeteorShower> result = service.getUpcomingShowers();

        assertEquals(1, result.size());
        assertEquals("Upcoming", result.get(0).name());

        verify(meteorShowersCache).get(CacheConfig.CACHE_KEY);
    }

    private MeteorShower createShower(String name, Instant visStart, Instant visEnd) {
        return new MeteorShower(
                name, name.substring(0, Math.min(3, name.length())).toUpperCase(Locale.ROOT),
                visStart.plus(Duration.ofDays(1)), visStart.plus(Duration.ofDays(2)),
                visStart, visEnd,
                50, null, null
        );
    }
}
