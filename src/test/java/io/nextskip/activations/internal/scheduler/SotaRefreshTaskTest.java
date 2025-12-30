package io.nextskip.activations.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.activations.internal.SotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Summit;
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
 * Unit tests for SotaRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class SotaRefreshTaskTest {

    @Mock
    private SotaClient sotaClient;

    @Mock
    private ActivationRepository repository;

    private SotaRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new SotaRefreshTask();
    }

    @Test
    void testSotaRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.sotaRecurringTask(sotaClient, repository);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("sota-refresh");
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository);

        verify(sotaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        Activation activation = createTestActivation();
        when(sotaClient.fetch()).thenReturn(List.of(activation));
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository);

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
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(3);

        task.executeRefresh(sotaClient, repository);

        verify(repository).deleteBySpottedAtBefore(any());
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(sotaClient.fetch()).thenThrow(new RuntimeException("SOTA API error"));

        assertThatThrownBy(() -> task.executeRefresh(sotaClient, repository))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SOTA refresh failed");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_SavesNothing() {
        when(sotaClient.fetch()).thenReturn(Collections.emptyList());
        when(repository.deleteBySpottedAtBefore(any())).thenReturn(0);

        task.executeRefresh(sotaClient, repository);

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
