package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.propagation.internal.HamQslClient;
import io.nextskip.propagation.internal.HamQslFetchResult;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for unified HamQslRefreshService.
 */
@ExtendWith(MockitoExtension.class)
class HamQslRefreshServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HamQslClient hamQslClient;

    @Mock
    private SolarIndicesRepository solarRepository;

    @Mock
    private BandConditionRepository bandRepository;

    @Mock
    private LoadingCache<String, SolarIndices> solarIndicesCache;

    @Mock
    private LoadingCache<String, List<BandCondition>> bandConditionsCache;

    @Captor
    private ArgumentCaptor<CacheRefreshEvent> eventCaptor;

    private HamQslRefreshService service;

    @BeforeEach
    void setUp() {
        service = new HamQslRefreshService(
                eventPublisher,
                hamQslClient,
                solarRepository,
                bandRepository,
                solarIndicesCache,
                bandConditionsCache
        );
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        HamQslFetchResult result = createTestFetchResult();
        when(hamQslClient.fetch()).thenReturn(result);

        service.executeRefresh();

        verify(hamQslClient).fetch();
    }

    @Test
    void testExecuteRefresh_SavesSolarIndices() {
        HamQslFetchResult result = createTestFetchResult();
        when(hamQslClient.fetch()).thenReturn(result);

        service.executeRefresh();

        ArgumentCaptor<SolarIndicesEntity> captor = ArgumentCaptor.forClass(SolarIndicesEntity.class);
        verify(solarRepository).save(captor.capture());

        SolarIndicesEntity saved = captor.getValue();
        assertThat(saved.getSolarFluxIndex()).isEqualTo(145.5);
        assertThat(saved.getAIndex()).isEqualTo(8);
        assertThat(saved.getKIndex()).isEqualTo(3);
        assertThat(saved.getSunspotNumber()).isEqualTo(115);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecuteRefresh_SavesBandConditions() {
        HamQslFetchResult result = createTestFetchResult();
        when(hamQslClient.fetch()).thenReturn(result);

        service.executeRefresh();

        ArgumentCaptor<List<BandConditionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(bandRepository).saveAll(captor.capture());

        List<BandConditionEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
    }

    @Test
    void testExecuteRefresh_PublishesCacheRefreshEvent() {
        HamQslFetchResult result = createTestFetchResult();
        when(hamQslClient.fetch()).thenReturn(result);

        service.executeRefresh();

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        CacheRefreshEvent event = eventCaptor.getValue();
        assertThat(event.cacheName()).isEqualTo("solarIndices+bandConditions");

        // Verify the refresh action calls both caches
        event.refreshAction().run();
        verify(solarIndicesCache).refresh(CacheConfig.CACHE_KEY);
        verify(bandConditionsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_NullResult_SkipsSave() {
        when(hamQslClient.fetch()).thenReturn(null);

        service.executeRefresh();

        verify(solarRepository, never()).save(any());
        verify(bandRepository, never()).saveAll(anyList());

        // Should still publish event but with skipped indication
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CacheRefreshEvent event = eventCaptor.getValue();
        assertThat(event.cacheName()).contains("skipped");
    }

    @Test
    void testExecuteRefresh_EmptyBandConditions_SavesSolarOnly() {
        SolarIndices solarIndices = createTestSolarIndices();
        HamQslFetchResult result = new HamQslFetchResult(solarIndices, List.of());
        when(hamQslClient.fetch()).thenReturn(result);

        service.executeRefresh();

        verify(solarRepository).save(any());
        verify(bandRepository, never()).saveAll(anyList());
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(hamQslClient.fetch()).thenThrow(new RuntimeException("HamQSL API error"));

        assertThatThrownBy(() -> service.executeRefresh())
                .isInstanceOf(RuntimeException.class);

        verify(solarRepository, never()).save(any());
        verify(bandRepository, never()).saveAll(anyList());
    }

    private HamQslFetchResult createTestFetchResult() {
        SolarIndices solarIndices = createTestSolarIndices();
        List<BandCondition> bandConditions = List.of(
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR)
        );
        return new HamQslFetchResult(solarIndices, bandConditions);
    }

    private SolarIndices createTestSolarIndices() {
        return new SolarIndices(
                145.5,
                8,
                3,
                115,
                Instant.now(),
                "HamQSL"
        );
    }
}
