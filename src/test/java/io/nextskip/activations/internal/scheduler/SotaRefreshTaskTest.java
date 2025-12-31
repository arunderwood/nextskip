package io.nextskip.activations.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.activations.internal.SotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Summit;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SotaRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Suppress warnings annotations are intentionally repeated
class SotaRefreshTaskTest {

    private static final String SOURCE_SOTA_API = "SOTA API";

    @Mock
    private SotaClient sotaClient;

    @Mock
    private ActivationRepository repository;

    @Mock
    private LoadingCache<String, List<Activation>> activationsCache;

    private SotaRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new SotaRefreshTask();
    }

    @Test
    void testSotaRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.sotaRecurringTask(sotaClient, repository, activationsCache);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("sota-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSotaRecurringTask_ExecuteHandler_InvokesRefresh() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_SOTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        RecurringTask<Void> recurringTask = task.sotaRecurringTask(sotaClient, repository, activationsCache);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(sotaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_SOTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository, activationsCache);

        verify(sotaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_SOTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository, activationsCache);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ActivationEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
    }

    @Test
    void testExecuteRefresh_CallsRepositoryDeleteOld() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_SOTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(3);

        task.executeRefresh(sotaClient, repository, activationsCache);

        verify(repository).deleteBySpottedAtBefore(any());
    }

    @Test
    void testExecuteRefresh_TriggersCacheRefresh() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_SOTA_API), anyList()))
                .thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository, activationsCache);

        verify(activationsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(sotaClient.fetch()).thenThrow(new RuntimeException("SOTA API error"));

        assertThatThrownBy(() -> task.executeRefresh(sotaClient, repository, activationsCache))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SOTA refresh failed");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_SavesNothing() {
        when(sotaClient.fetch()).thenReturn(Collections.emptyList());
        // Note: findBySourceAndSpotIdIn is not called for empty list (early return in helper)
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository, activationsCache);

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
        existingEntity.setId(99L);

        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_SOTA_API), anyList()))
                .thenReturn(List.of(existingEntity));
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        // When: Executing the refresh
        task.executeRefresh(sotaClient, repository, activationsCache);

        // Then: The saved entity should have the existing ID (for UPDATE)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ActivationEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo(99L);
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                any(ActivationType.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasRecentData_ReturnsFalse() {
        ActivationEntity entity = createTestEntity();
        when(repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                any(ActivationType.class), any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    private Activation createTestActivation() {
        Summit summit = new Summit(
                "W4C/WM-001",
                "Test Summit",
                "NC",
                "W4C"
        );
        return new Activation(
                "spot-5678",
                "W4ABC",
                ActivationType.SOTA,
                14285.0,
                "SSB",
                Instant.now(),
                10,
                "SOTA",
                summit
        );
    }

    private ActivationEntity createTestEntity() {
        Instant now = Instant.now();
        return new ActivationEntity(
                "spot-5678",
                "W4ABC",
                ActivationType.SOTA,
                14285.0,
                "SSB",
                now,
                10,
                "SOTA",
                "W4C/WM-001",
                "Test Summit",
                "NC",
                null,
                null,
                null,
                null,
                "W4C"
        );
    }
}
