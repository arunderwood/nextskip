package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
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
 * Unit tests for HamQslSolarRefreshTask.
 *
 * <p>Tests the task configuration and initial load detection.
 * Business logic tests are in {@link HamQslSolarRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class HamQslSolarRefreshTaskTest {

    @Mock
    private HamQslSolarRefreshService refreshService;

    @Mock
    private SolarIndicesRepository repository;

    private HamQslSolarRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new HamQslSolarRefreshTask(repository);
    }

    @Test
    void testHamQslSolarRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.hamQslSolarRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("hamqsl-solar-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHamQslSolarRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.hamQslSolarRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasHamQslData_ReturnsFalse() {
        SolarIndicesEntity entity = createTestEntity("HamQSL");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_OnlyNoaaData_ReturnsTrue() {
        SolarIndicesEntity entity = createTestEntity("NOAA SWPC");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testGetTaskName_ReturnsHamQslSolar() {
        assertThat(task.getTaskName()).isEqualTo("HamQSL Solar");
    }

    @Test
    void testGetRecurringTask_AfterSetterCalled_ReturnsTask() {
        RecurringTask<Void> recurringTask = task.hamQslSolarRecurringTask(refreshService);
        task.setRecurringTask(recurringTask);

        assertThat(task.getRecurringTask()).isSameAs(recurringTask);
    }

    private SolarIndicesEntity createTestEntity(String source) {
        return new SolarIndicesEntity(
                145.0,
                12,
                4,
                95,
                Instant.now(),
                source
        );
    }
}
