package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.propagation.internal.HamQslBandClient;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HamQslBandRefreshService.
 */
@ExtendWith(MockitoExtension.class)
class HamQslBandRefreshServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HamQslBandClient hamQslBandClient;

    @Mock
    private BandConditionRepository repository;

    @Mock
    private LoadingCache<String, List<BandCondition>> bandConditionsCache;

    private HamQslBandRefreshService service;

    @BeforeEach
    void setUp() {
        service = new HamQslBandRefreshService(eventPublisher, hamQslBandClient, repository, bandConditionsCache);
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        service.executeRefresh();

        verify(hamQslBandClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BandConditionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<BandConditionEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
    }

    @Test
    void testExecuteRefresh_PublishesCacheRefreshEvent() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        service.executeRefresh();

        // Verify event is published
        ArgumentCaptor<CacheRefreshEvent> captor = ArgumentCaptor.forClass(CacheRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        CacheRefreshEvent event = captor.getValue();
        assertThat(event.cacheName()).isEqualTo("bandConditions");

        // Verify the refresh action calls the cache
        event.refreshAction().run();
        verify(bandConditionsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(hamQslBandClient.fetch()).thenThrow(new RuntimeException("HamQSL Band API error"));

        assertThatThrownBy(() -> service.executeRefresh())
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_SavesNothing() {
        when(hamQslBandClient.fetch()).thenReturn(Collections.emptyList());

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BandConditionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    private List<BandCondition> createTestConditions() {
        return List.of(
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR)
        );
    }
}
