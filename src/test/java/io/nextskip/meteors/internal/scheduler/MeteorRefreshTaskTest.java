package io.nextskip.meteors.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MeteorRefreshTask.
 *
 * <p>Tests the task configuration and initial load detection.
 * Business logic tests are in {@link MeteorRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class MeteorRefreshTaskTest {

    @Mock
    private MeteorRefreshService refreshService;

    @Mock
    private MeteorShowerRepository repository;

    private MeteorRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new MeteorRefreshTask(repository);
    }

    @Test
    void testMeteorRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.meteorRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("meteor-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMeteorRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.meteorRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_NoActiveOrUpcoming_ReturnsTrue() {
        when(repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasActiveShower_ReturnsFalse() {
        MeteorShowerEntity entity = createTestEntity();
        when(repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_HasUpcomingShower_ReturnsFalse() {
        when(repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        MeteorShowerEntity entity = createTestEntity();
        when(repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testGetTaskName_ReturnsMeteorRefresh() {
        assertThat(task.getTaskName()).isEqualTo("meteor-refresh");
    }

    @Test
    void testGetDisplayName_ReturnsMeteor() {
        assertThat(task.getDisplayName()).isEqualTo("Meteor");
    }

    @Test
    void testGetRecurringTask_AfterSetterCalled_ReturnsTask() {
        RecurringTask<Void> recurringTask = task.meteorRecurringTask(refreshService);
        task.setRecurringTask(recurringTask);

        assertThat(task.getRecurringTask()).isSameAs(recurringTask);
    }

    private MeteorShowerEntity createTestEntity() {
        Instant now = Instant.now();
        return new MeteorShowerEntity(
                "Perseids 2025",
                "PER",
                now.plus(10, ChronoUnit.DAYS),
                now.plus(11, ChronoUnit.DAYS),
                now.minus(5, ChronoUnit.DAYS),
                now.plus(20, ChronoUnit.DAYS),
                100,
                "109P/Swift-Tuttle",
                "https://example.com/perseids"
        );
    }
}
