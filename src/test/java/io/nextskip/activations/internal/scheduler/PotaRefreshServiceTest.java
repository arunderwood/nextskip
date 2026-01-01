package io.nextskip.activations.internal.scheduler;

import static io.nextskip.test.fixtures.ActivationFixtures.pota;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.internal.PotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.config.CacheConfig;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PotaRefreshService.
 */
@ExtendWith(MockitoExtension.class)
class PotaRefreshServiceTest {

    private static final String SOURCE_POTA_API = "POTA API";

    @Mock
    private PotaClient potaClient;

    @Mock
    private ActivationRepository repository;

    @Mock
    private LoadingCache<String, List<Activation>> activationsCache;

    private PotaRefreshService service;

    @BeforeEach
    void setUp() {
        service = new PotaRefreshService(potaClient, repository, activationsCache);
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any())).thenReturn(0);

        service.executeRefresh();

        verify(potaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any())).thenReturn(0);

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ActivationEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
    }

    @Test
    void testExecuteRefresh_CallsRepositoryDeleteOld() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any())).thenReturn(5);

        service.executeRefresh();

        verify(repository).deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any());
    }

    @Test
    void testExecuteRefresh_TriggersCacheRefresh() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any())).thenReturn(0);

        service.executeRefresh();

        verify(activationsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(potaClient.fetch()).thenThrow(new RuntimeException("POTA API error"));

        assertThatThrownBy(() -> service.executeRefresh())
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_SavesNothing() {
        when(potaClient.fetch()).thenReturn(Collections.emptyList());
        when(repository.deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any())).thenReturn(0);

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testExecuteRefresh_ExistingActivation_SetsIdForUpdate() {
        // Given: An activation that already exists in the database
        Activation activation = createTestActivation();
        ActivationEntity existingEntity = createTestEntity();
        existingEntity.setId(42L);

        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA_API), anyList()))
                .thenReturn(List.of(existingEntity));
        when(repository.deleteBySourceAndSpottedAtBefore(eq(SOURCE_POTA_API), any())).thenReturn(0);

        // When: Executing the refresh
        service.executeRefresh();

        // Then: The saved entity should have the existing ID (for UPDATE)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ActivationEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo(42L);
    }

    private Activation createTestActivation() {
        return pota().spotId("spot-1234").build();
    }

    private ActivationEntity createTestEntity() {
        Instant now = Instant.now();
        return new ActivationEntity(
                "spot-1234",
                "W1ABC",
                ActivationType.POTA,
                14074.0,
                "FT8",
                now,
                5,
                "POTA",
                "K-1234",
                "Test Park",
                "CO",
                "US",
                "DM79",
                40.0,
                -105.0,
                null
        );
    }
}
