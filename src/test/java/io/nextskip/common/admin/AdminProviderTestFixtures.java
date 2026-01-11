package io.nextskip.common.admin;

import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

import java.time.Instant;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared test fixtures for admin provider tests.
 *
 * <p>Provides factory methods for creating mock scheduler executions
 * to reduce duplication across admin provider test classes.
 */
public final class AdminProviderTestFixtures {

    private AdminProviderTestFixtures() {
        // Utility class
    }

    /**
     * Creates a mock ScheduledExecution with the specified parameters.
     *
     * @param taskName the task name
     * @param executionTime the scheduled execution time
     * @param isPicked whether the task is currently being executed
     * @param consecutiveFailures number of consecutive failures
     * @param lastFailure time of last failure (null if no failures)
     * @return a mock ScheduledExecution
     */
    @SuppressWarnings("unchecked")
    public static ScheduledExecution<Object> createMockExecution(
            String taskName,
            Instant executionTime,
            boolean isPicked,
            int consecutiveFailures,
            Instant lastFailure) {

        TaskInstance<Object> taskInstance = mock(TaskInstance.class);
        when(taskInstance.getTaskName()).thenReturn(taskName);

        ScheduledExecution<Object> execution = mock(ScheduledExecution.class);
        when(execution.getTaskInstance()).thenReturn(taskInstance);
        when(execution.getExecutionTime()).thenReturn(executionTime);
        when(execution.isPicked()).thenReturn(isPicked);
        when(execution.getConsecutiveFailures()).thenReturn(consecutiveFailures);
        lenient().when(execution.getLastFailure()).thenReturn(lastFailure);

        return execution;
    }

    /**
     * Creates a healthy mock execution (no failures, not currently running).
     *
     * @param taskName the task name
     * @param nextExecutionTime the next scheduled execution time
     * @return a mock ScheduledExecution in healthy state
     */
    public static ScheduledExecution<Object> createHealthyExecution(String taskName, Instant nextExecutionTime) {
        return createMockExecution(taskName, nextExecutionTime, false, 0, null);
    }

    /**
     * Creates a currently running mock execution.
     *
     * @param taskName the task name
     * @return a mock ScheduledExecution that is currently running
     */
    public static ScheduledExecution<Object> createRunningExecution(String taskName) {
        return createMockExecution(taskName, Instant.now(), true, 0, null);
    }

    /**
     * Creates a degraded mock execution (1-2 consecutive failures).
     *
     * @param taskName the task name
     * @param nextExecutionTime the next scheduled execution time
     * @param failures number of consecutive failures (1-2)
     * @param lastFailure time of last failure
     * @return a mock ScheduledExecution in degraded state
     */
    public static ScheduledExecution<Object> createDegradedExecution(
            String taskName,
            Instant nextExecutionTime,
            int failures,
            Instant lastFailure) {
        return createMockExecution(taskName, nextExecutionTime, false, failures, lastFailure);
    }

    /**
     * Creates an unhealthy mock execution (3+ consecutive failures).
     *
     * @param taskName the task name
     * @param nextExecutionTime the next scheduled execution time
     * @param failures number of consecutive failures (3+)
     * @param lastFailure time of last failure
     * @return a mock ScheduledExecution in unhealthy state
     */
    public static ScheduledExecution<Object> createUnhealthyExecution(
            String taskName,
            Instant nextExecutionTime,
            int failures,
            Instant lastFailure) {
        return createMockExecution(taskName, nextExecutionTime, false, failures, lastFailure);
    }
}
