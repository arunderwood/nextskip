package io.nextskip.activations.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SotaRefreshTask.
 *
 * <p>Tests the task configuration and initial load detection.
 * Business logic tests are in {@link SotaRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class SotaRefreshTaskTest {

    @Mock
    private SotaRefreshService refreshService;

    @Mock
    private ActivationRepository repository;

    private SotaRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new SotaRefreshTask(repository);
    }

    @Test
    void testSotaRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.sotaRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("sota-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSotaRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.sotaRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                any(ActivationType.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasRecentData_ReturnsFalse() {
        ActivationEntity entity = createTestEntity();
        when(repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                any(ActivationType.class), any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testGetTaskName_ReturnsSota() {
        assertThat(task.getTaskName()).isEqualTo("SOTA");
    }

    @Test
    void testGetRecurringTask_AfterSetterCalled_ReturnsTask() {
        RecurringTask<Void> recurringTask = task.sotaRecurringTask(refreshService);
        task.setRecurringTask(recurringTask);

        assertThat(task.getRecurringTask()).isSameAs(recurringTask);
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
