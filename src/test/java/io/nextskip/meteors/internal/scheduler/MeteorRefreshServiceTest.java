package io.nextskip.meteors.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.meteors.internal.MeteorShowerDataLoader;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MeteorRefreshService.
 */
@ExtendWith(MockitoExtension.class)
class MeteorRefreshServiceTest {

    @Mock
    private MeteorShowerDataLoader dataLoader;

    @Mock
    private MeteorShowerRepository repository;

    @Mock
    private LoadingCache<String, List<MeteorShower>> meteorShowersCache;

    private MeteorRefreshService service;

    @BeforeEach
    void setUp() {
        service = new MeteorRefreshService(dataLoader, repository, meteorShowersCache);
    }

    @Test
    void testExecuteRefresh_CallsDataLoader() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        service.executeRefresh();

        verify(dataLoader).getShowers(30);
    }

    @Test
    void testExecuteRefresh_CallsRepositoryDeleteAll() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        service.executeRefresh();

        verify(repository).deleteAll();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MeteorShowerEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<MeteorShowerEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getName()).isEqualTo("Perseids 2025");
    }

    @Test
    void testExecuteRefresh_TriggersCacheRefresh() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        service.executeRefresh();

        verify(meteorShowersCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_DataLoaderThrowsException_PropagatesException() {
        when(dataLoader.getShowers(anyInt())).thenThrow(new RuntimeException("Data loader error"));

        assertThatThrownBy(() -> service.executeRefresh())
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).deleteAll();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_DeletesAllAndSavesNothing() {
        when(dataLoader.getShowers(anyInt())).thenReturn(Collections.emptyList());

        service.executeRefresh();

        verify(repository).deleteAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MeteorShowerEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    private List<MeteorShower> createTestShowers() {
        Instant now = Instant.now();
        return List.of(new MeteorShower(
                "Perseids 2025",
                "PER",
                now.plus(10, ChronoUnit.DAYS),
                now.plus(11, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS),
                now.plus(20, ChronoUnit.DAYS),
                100,
                "109P/Swift-Tuttle",
                "https://example.com/perseids"
        ));
    }
}
