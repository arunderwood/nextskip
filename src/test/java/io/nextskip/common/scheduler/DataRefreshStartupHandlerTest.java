package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DataRefreshStartupHandler.
 *
 * <p>Tests the coordinator-based startup handling that automatically discovers
 * and processes all RefreshTaskCoordinator implementations.
 */
@ExtendWith(MockitoExtension.class)
class DataRefreshStartupHandlerTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private RefreshTaskCoordinator coordinator1;

    @Mock
    private RefreshTaskCoordinator coordinator2;

    @Mock
    private RefreshTaskCoordinator coordinator3;

    @Mock
    private RecurringTask<Void> recurringTask1;

    @Mock
    private RecurringTask<Void> recurringTask2;

    @Mock
    private RecurringTask<Void> recurringTask3;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @Mock
    private TaskInstance<Void> taskInstance;

    @Test
    void testOnApplicationReady_EagerLoadDisabled_SkipsAllChecks() {
        DataRefreshStartupHandler handler = createHandler(false, List.of(coordinator1));

        handler.onApplicationReady(applicationReadyEvent);

        verify(coordinator1, never()).needsInitialLoad();
        verify(scheduler, never()).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_NoCoordinators_NoReschedules() {
        DataRefreshStartupHandler handler = createHandler(true, List.of());

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler, never()).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_AllCoordinatorsHaveData_NoReschedules() {
        when(coordinator1.needsInitialLoad()).thenReturn(false);
        when(coordinator2.needsInitialLoad()).thenReturn(false);

        DataRefreshStartupHandler handler = createHandler(true, List.of(coordinator1, coordinator2));

        handler.onApplicationReady(applicationReadyEvent);

        verify(coordinator1).needsInitialLoad();
        verify(coordinator2).needsInitialLoad();
        verify(scheduler, never()).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_SingleCoordinatorNeedsLoad_Reschedules() {
        when(coordinator1.needsInitialLoad()).thenReturn(true);
        when(coordinator1.getRecurringTask()).thenReturn(recurringTask1);
        when(coordinator1.getTaskName()).thenReturn("Test Task");
        when(recurringTask1.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        DataRefreshStartupHandler handler = createHandler(true, List.of(coordinator1));

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_MultipleCoordinatorsNeedLoad_ReschedulesAll() {
        when(coordinator1.needsInitialLoad()).thenReturn(true);
        when(coordinator1.getRecurringTask()).thenReturn(recurringTask1);
        when(coordinator1.getTaskName()).thenReturn("Task 1");
        when(recurringTask1.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        when(coordinator2.needsInitialLoad()).thenReturn(false);

        when(coordinator3.needsInitialLoad()).thenReturn(true);
        when(coordinator3.getRecurringTask()).thenReturn(recurringTask3);
        when(coordinator3.getTaskName()).thenReturn("Task 3");
        when(recurringTask3.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        DataRefreshStartupHandler handler = createHandler(
                true, List.of(coordinator1, coordinator2, coordinator3));

        handler.onApplicationReady(applicationReadyEvent);

        // Verify reschedule was called twice (coordinator1 and coordinator3)
        verify(scheduler, times(2)).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_RescheduleThrowsException_ContinuesProcessing() {
        when(coordinator1.needsInitialLoad()).thenReturn(true);
        when(coordinator1.getRecurringTask()).thenReturn(recurringTask1);
        when(coordinator1.getTaskName()).thenReturn("Failing Task");
        when(recurringTask1.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);
        when(scheduler.reschedule(any(), any(Instant.class)))
                .thenThrow(new RuntimeException("Scheduler error"));

        when(coordinator2.needsInitialLoad()).thenReturn(true);
        when(coordinator2.getRecurringTask()).thenReturn(recurringTask2);
        when(coordinator2.getTaskName()).thenReturn("Second Task");
        when(recurringTask2.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        DataRefreshStartupHandler handler = createHandler(true, List.of(coordinator1, coordinator2));

        // Should not throw - exception is caught and logged, then continues to next coordinator
        handler.onApplicationReady(applicationReadyEvent);

        // Both coordinators should have been attempted
        verify(scheduler, times(2)).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_MixedCoordinatorStates_ReschedulesOnlyNeeded() {
        // First coordinator has data
        when(coordinator1.needsInitialLoad()).thenReturn(false);

        // Second coordinator needs load
        when(coordinator2.needsInitialLoad()).thenReturn(true);
        when(coordinator2.getRecurringTask()).thenReturn(recurringTask2);
        when(coordinator2.getTaskName()).thenReturn("Needs Load");
        when(recurringTask2.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        // Third coordinator has data
        when(coordinator3.needsInitialLoad()).thenReturn(false);

        DataRefreshStartupHandler handler = createHandler(
                true, List.of(coordinator1, coordinator2, coordinator3));

        handler.onApplicationReady(applicationReadyEvent);

        // Only coordinator2 should trigger reschedule
        verify(scheduler, times(1)).reschedule(any(), any(Instant.class));
        verify(coordinator2).getRecurringTask();
    }

    private DataRefreshStartupHandler createHandler(
            boolean eagerLoadEnabled, List<RefreshTaskCoordinator> coordinators) {
        return new DataRefreshStartupHandler(scheduler, eagerLoadEnabled, coordinators);
    }
}
