package io.nextskip.activations.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.activations.internal.PotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PotaRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class PotaRefreshTaskTest {

    @Mock
    private PotaClient potaClient;

    @Mock
    private ActivationRepository repository;

    private PotaRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new PotaRefreshTask();
    }

    @Test
    void testPotaRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.potaRecurringTask(potaClient, repository);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("pota-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPotaRecurringTask_ExecuteHandler_InvokesRefresh() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        RecurringTask<Void> recurringTask = task.potaRecurringTask(potaClient, repository);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        // This invokes the lambda, providing coverage for line 52
        recurringTask.execute(taskInstance, executionContext);

        verify(potaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(potaClient, repository);

        verify(potaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        Activation activation = createTestActivation();
        when(potaClient.fetch()).thenReturn(List.of(activation));
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(potaClient, repository);

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
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(5);

        task.executeRefresh(potaClient, repository);

        verify(repository).deleteBySpottedAtBefore(any());
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(potaClient.fetch()).thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> task.executeRefresh(potaClient, repository))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("POTA refresh failed");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_SavesNothing() {
        when(potaClient.fetch()).thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(potaClient, repository);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
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
        Park park = new Park(
                "K-1234",
                "Test Park",
                "CO",
                "US",
                "DM79",
                40.0,
                -105.0
        );
        return new Activation(
                "spot-1234",
                "W1ABC",
                ActivationType.POTA,
                14074.0,
                "FT8",
                Instant.now(),
                5,
                "POTA",
                park
        );
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
