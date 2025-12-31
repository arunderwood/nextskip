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
 * Unit tests for NoaaRefreshTask.
 *
 * <p>Tests the task configuration and initial load detection.
 * Business logic tests are in {@link NoaaRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class NoaaRefreshTaskTest {

    @Mock
    private NoaaRefreshService refreshService;

    @Mock
    private SolarIndicesRepository repository;

    private NoaaRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new NoaaRefreshTask();
    }

    @Test
    void testNoaaRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.noaaRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("noaa-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNoaaRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.noaaRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasNoaaData_ReturnsFalse() {
        SolarIndicesEntity entity = createTestEntity("NOAA SWPC");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_OnlyHamQslData_ReturnsTrue() {
        SolarIndicesEntity entity = createTestEntity("HamQSL");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    private SolarIndicesEntity createTestEntity(String source) {
        return new SolarIndicesEntity(
                150.0,
                10,
                3,
                100,
                Instant.now(),
                source
        );
    }
}
