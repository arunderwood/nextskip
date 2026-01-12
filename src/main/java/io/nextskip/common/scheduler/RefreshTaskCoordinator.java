package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;

/**
 * Coordinates a recurring refresh task with its startup behavior.
 *
 * <p>Implementations encapsulate:
 * <ul>
 *   <li>The RecurringTask bean for db-scheduler</li>
 *   <li>Logic to determine if initial data load is needed</li>
 *   <li>Task name for logging and identification</li>
 * </ul>
 *
 * <p>This interface follows the Open-Closed Principle: adding a new refresh task
 * requires only implementing this interface, with no changes to DataRefreshStartupHandler.
 *
 * @see DataRefreshStartupHandler
 */
public interface RefreshTaskCoordinator {

    /**
     * Returns the recurring task bean for this coordinator.
     *
     * @return the configured RecurringTask
     */
    RecurringTask<Void> getRecurringTask();

    /**
     * Checks if this task needs immediate execution due to empty or stale data.
     *
     * <p>Called by {@link DataRefreshStartupHandler} on application startup
     * to determine if the task should be rescheduled for immediate execution.
     *
     * @return true if the task should be rescheduled for immediate execution
     */
    boolean needsInitialLoad();

    /**
     * Returns a human-readable name for logging.
     *
     * @return the task name (e.g., "POTA", "NOAA")
     */
    String getTaskName();

    /**
     * Returns a human-readable display name for the admin UI.
     *
     * <p>Override this method to provide a user-friendly name (e.g., "NOAA Solar Indices"
     * instead of "noaa-refresh").
     *
     * @return the display name for admin UI, defaults to getTaskName()
     */
    default String getDisplayName() {
        return getTaskName();
    }
}
