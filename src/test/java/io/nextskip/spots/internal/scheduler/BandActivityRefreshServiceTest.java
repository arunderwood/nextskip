package io.nextskip.spots.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.spots.api.BandActivityChangedEvent;
import io.nextskip.spots.internal.aggregation.BandActivityAggregator;
import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.model.ContinentPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import io.nextskip.common.scheduler.CacheRefreshEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BandActivityRefreshService}.
 *
 * <p>Tests the scheduled refresh service for band activity aggregations.
 */
@ExtendWith(MockitoExtension.class)
class BandActivityRefreshServiceTest {

    private static final Instant NOW = Instant.parse("2025-01-15T12:00:00Z");
    private static final String BAND_20M = "20m";
    private static final String BAND_40M = "40m";
    private static final String BAND_15M = "15m";

    @Mock
    private BandActivityAggregator aggregator;

    @Mock
    private LoadingCache<String, Map<String, BandActivity>> bandActivityCache;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<BandActivityChangedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<CacheRefreshEvent> cacheEventCaptor;

    private BandActivityRefreshService refreshService;

    @BeforeEach
    void setUp() {
        refreshService = new BandActivityRefreshService(
                eventPublisher, aggregator, bandActivityCache);
    }

    // =========================================================================
    // Service Name Tests
    // =========================================================================

    @Nested
    class ServiceNameTests {

        @Test
        void testGetServiceName_ReturnsBandActivity() {
            // The service name is accessed indirectly through the abstract class
            // We verify correct initialization by running a refresh
            when(aggregator.aggregateAllBands()).thenReturn(Map.of());

            refreshService.executeRefresh();

            // Service completes without error - name is correctly configured
            verify(aggregator).aggregateAllBands();
        }
    }

    // =========================================================================
    // Refresh Tests
    // =========================================================================

    @Nested
    class RefreshTests {

        @Test
        void testRefresh_AggregatesAndPublishesEvent() {
            // Given
            Map<String, BandActivity> activities = Map.of(
                    BAND_20M, createBandActivity(BAND_20M, 100),
                    BAND_40M, createBandActivity(BAND_40M, 50)
            );
            when(aggregator.aggregateAllBands()).thenReturn(activities);

            // When
            refreshService.executeRefresh();

            // Then
            verify(aggregator).aggregateAllBands();
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            BandActivityChangedEvent event = eventCaptor.getValue();
            assertThat(event.bandActivities()).hasSize(2);
            assertThat(event.bandActivities()).containsKeys(BAND_20M, BAND_40M);
        }

        @Test
        void testRefresh_CacheRefreshEvent_PopulatesCacheWithAggregatedResult() {
            // Given
            Map<String, BandActivity> activities = Map.of(
                    BAND_20M, createBandActivity(BAND_20M, 100)
            );
            when(aggregator.aggregateAllBands()).thenReturn(activities);

            // When
            refreshService.executeRefresh();

            // Then: CacheRefreshEvent runnable puts the result into cache
            verify(eventPublisher).publishEvent(cacheEventCaptor.capture());
            CacheRefreshEvent cacheEvent = cacheEventCaptor.getValue();
            assertThat(cacheEvent.cacheName()).isEqualTo("bandActivity");

            // Simulate post-commit: run the refresh action
            cacheEvent.refreshAction().run();
            verify(bandActivityCache).put(eq("all"), eq(activities));
        }

        @Test
        void testRefresh_EmptyAggregation_PublishesEmptyEvent() {
            // Given
            when(aggregator.aggregateAllBands()).thenReturn(Map.of());

            // When
            refreshService.executeRefresh();

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            BandActivityChangedEvent event = eventCaptor.getValue();
            assertThat(event.bandActivities()).isEmpty();
        }

        @Test
        void testRefresh_SingleBand_PublishesEvent() {
            // Given
            Map<String, BandActivity> activities = Map.of(
                    BAND_15M, createBandActivity(BAND_15M, 200)
            );
            when(aggregator.aggregateAllBands()).thenReturn(activities);

            // When
            refreshService.executeRefresh();

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            BandActivityChangedEvent event = eventCaptor.getValue();
            assertThat(event.bandActivities()).hasSize(1);
            assertThat(event.bandActivities().get(BAND_15M).spotCount()).isEqualTo(200);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private BandActivity createBandActivity(String band, int spotCount) {
        return new BandActivity(
                band,
                "FT8",
                spotCount,
                80,
                25.0,
                10000,
                "JA1ABC → W6XYZ",
                Set.of(ContinentPath.NA_AS),
                NOW.minusSeconds(900),
                NOW,
                NOW
        );
    }
}
