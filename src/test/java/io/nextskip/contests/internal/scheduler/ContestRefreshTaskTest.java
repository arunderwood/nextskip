package io.nextskip.contests.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestRefreshTask.
 *
 * <p>Tests the task configuration and initial load detection.
 * Business logic tests are in {@link ContestRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class ContestRefreshTaskTest {

    @Mock
    private ContestRefreshService refreshService;

    @Mock
    private ContestRepository repository;

    private ContestRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new ContestRefreshTask(repository);
    }

    @Test
    void testContestRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.contestRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("contest-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testContestRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.contestRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByEndTimeAfterOrderByStartTimeAsc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasFutureContests_ReturnsFalse() {
        ContestEntity entity = createTestEntity();
        when(repository.findByEndTimeAfterOrderByStartTimeAsc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testGetTaskName_ReturnsContest() {
        assertThat(task.getTaskName()).isEqualTo("Contest");
    }

    @Test
    void testGetRecurringTask_AfterSetterCalled_ReturnsTask() {
        RecurringTask<Void> recurringTask = task.contestRecurringTask(refreshService);
        task.setRecurringTask(recurringTask);

        assertThat(task.getRecurringTask()).isSameAs(recurringTask);
    }

    private ContestEntity createTestEntity() {
        Instant now = Instant.now();
        return new ContestEntity(
                "Test Contest",
                now.plus(1, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS),
                Set.of(),
                Set.of(),
                "ARRL",
                "https://example.com",
                null
        );
    }
}
