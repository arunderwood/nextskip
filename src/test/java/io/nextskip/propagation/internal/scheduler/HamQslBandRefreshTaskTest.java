package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
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
 * Unit tests for HamQslBandRefreshTask.
 *
 * <p>Tests the task configuration and initial load detection.
 * Business logic tests are in {@link HamQslBandRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class HamQslBandRefreshTaskTest {

    @Mock
    private HamQslBandRefreshService refreshService;

    @Mock
    private BandConditionRepository repository;

    private HamQslBandRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new HamQslBandRefreshTask(repository);
    }

    @Test
    void testHamQslBandRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.hamQslBandRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("hamqsl-band-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHamQslBandRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.hamQslBandRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasRecentData_ReturnsFalse() {
        BandConditionEntity entity = createTestEntity();
        when(repository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testGetTaskName_ReturnsHamQslBand() {
        assertThat(task.getTaskName()).isEqualTo("HamQSL Band");
    }

    @Test
    void testGetRecurringTask_AfterSetterCalled_ReturnsTask() {
        RecurringTask<Void> recurringTask = task.hamQslBandRecurringTask(refreshService);
        task.setRecurringTask(recurringTask);

        assertThat(task.getRecurringTask()).isSameAs(recurringTask);
    }

    private BandConditionEntity createTestEntity() {
        return new BandConditionEntity(
                FrequencyBand.BAND_20M,
                BandConditionRating.GOOD,
                1.0,
                null,
                Instant.now()
        );
    }
}
