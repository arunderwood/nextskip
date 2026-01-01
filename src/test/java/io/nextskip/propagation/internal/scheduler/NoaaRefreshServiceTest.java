package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.propagation.internal.NoaaSwpcClient;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NoaaRefreshService.
 */
@ExtendWith(MockitoExtension.class)
class NoaaRefreshServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NoaaSwpcClient noaaClient;

    @Mock
    private SolarIndicesRepository repository;

    @Mock
    private LoadingCache<String, SolarIndices> solarIndicesCache;

    private NoaaRefreshService service;

    @BeforeEach
    void setUp() {
        service = new NoaaRefreshService(eventPublisher, noaaClient, repository, solarIndicesCache);
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        SolarIndices indices = createTestSolarIndices();
        when(noaaClient.fetch()).thenReturn(indices);

        service.executeRefresh();

        verify(noaaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySave() {
        SolarIndices indices = createTestSolarIndices();
        when(noaaClient.fetch()).thenReturn(indices);

        service.executeRefresh();

        ArgumentCaptor<SolarIndicesEntity> captor = ArgumentCaptor.forClass(SolarIndicesEntity.class);
        verify(repository).save(captor.capture());

        SolarIndicesEntity saved = captor.getValue();
        assertThat(saved.getSolarFluxIndex()).isEqualTo(150.0);
        assertThat(saved.getSunspotNumber()).isEqualTo(100);
    }

    @Test
    void testExecuteRefresh_PublishesCacheRefreshEvent() {
        SolarIndices indices = createTestSolarIndices();
        when(noaaClient.fetch()).thenReturn(indices);

        service.executeRefresh();

        // Verify event is published
        ArgumentCaptor<CacheRefreshEvent> captor = ArgumentCaptor.forClass(CacheRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        CacheRefreshEvent event = captor.getValue();
        assertThat(event.cacheName()).isEqualTo("solarIndices");

        // Verify the refresh action calls the cache
        event.refreshAction().run();
        verify(solarIndicesCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(noaaClient.fetch()).thenThrow(new RuntimeException("NOAA API error"));

        assertThatThrownBy(() -> service.executeRefresh())
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).save(any());
    }

    private SolarIndices createTestSolarIndices() {
        return new SolarIndices(
                150.0,
                10,
                3,
                100,
                Instant.now(),
                "NOAA SWPC"
        );
    }
}
