package io.nextskip.common.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract base class for AdminStatusProvider implementations that use scheduled feeds.
 *
 * <p>Provides common functionality for:
 * <ul>
 *   <li>Building feed status from db-scheduler executions</li>
 *   <li>Triggering manual refresh by rescheduling tasks</li>
 *   <li>Querying the scheduler for execution state</li>
 * </ul>
 *
 * <p>Subclasses need only define their feed configurations via the constructor.
 * For providers with mixed feed types (scheduled + subscription), override
 * {@link #handleNonScheduledFeed(String)} and {@link #getFeedStatuses()}.
 *
 * @see ScheduledFeedDefinition
 */
public abstract class AbstractScheduledAdminProvider implements AdminStatusProvider {

    /** The db-scheduler instance for querying and rescheduling tasks. */
    protected final Scheduler scheduler;

    private final String moduleName;
    private final List<ScheduledFeedDefinition> feedDefinitions;
    private final Map<String, String> feedToTaskMap;

    /**
     * Creates a new AbstractScheduledAdminProvider.
     *
     * @param scheduler The db-scheduler instance
     * @param moduleName The module name for grouping in admin UI
     * @param feedDefinitions The list of scheduled feed definitions
     */
    protected AbstractScheduledAdminProvider(
            Scheduler scheduler,
            String moduleName,
            List<ScheduledFeedDefinition> feedDefinitions) {
        this.scheduler = scheduler;
        this.moduleName = moduleName;
        this.feedDefinitions = List.copyOf(feedDefinitions);
        this.feedToTaskMap = buildFeedToTaskMap(feedDefinitions);
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public List<FeedStatus> getFeedStatuses() {
        return feedDefinitions.stream()
                .map(this::buildScheduledFeedStatus)
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Scheduler can throw various runtime exceptions
    public Optional<TriggerRefreshResult> triggerRefresh(String feedName) {
        // Check for non-scheduled feeds first (hook for mixed-type providers)
        Optional<TriggerRefreshResult> nonScheduled = handleNonScheduledFeed(feedName);
        if (nonScheduled.isPresent()) {
            return nonScheduled;
        }

        String taskName = feedToTaskMap.get(feedName);
        if (taskName == null) {
            return Optional.empty();
        }

        try {
            Instant now = Instant.now();
            scheduler.reschedule(
                    TaskInstanceId.of(taskName, "recurring"),
                    now
            );
            return Optional.of(TriggerRefreshResult.success(feedName, now));
        } catch (Exception e) {
            return Optional.of(new TriggerRefreshResult(
                    false,
                    "Failed to trigger refresh: " + e.getMessage(),
                    feedName,
                    null
            ));
        }
    }

    /**
     * Hook for handling non-scheduled feeds (e.g., subscription feeds).
     *
     * <p>Override this method in providers that have mixed feed types.
     * Return {@link Optional#of} with an appropriate result for subscription feeds,
     * or {@link Optional#empty()} to continue with scheduled feed handling.
     *
     * @param feedName The feed name being refreshed
     * @return Optional result if this is a non-scheduled feed, empty otherwise
     */
    protected Optional<TriggerRefreshResult> handleNonScheduledFeed(String feedName) {
        return Optional.empty();
    }

    /**
     * Builds a ScheduledFeedStatus from a feed definition.
     *
     * <p>Queries the scheduler for the current execution state and calculates
     * last refresh time based on next execution minus interval.
     *
     * @param definition The feed definition
     * @return The current status of the scheduled feed
     */
    protected ScheduledFeedStatus buildScheduledFeedStatus(ScheduledFeedDefinition definition) {
        Optional<ScheduledExecution<Object>> execution = findScheduledExecution(definition.taskName());

        Instant lastRefreshTime = null;
        Instant nextRefreshTime = null;
        boolean isCurrentlyRefreshing = false;
        int consecutiveFailures = 0;
        Instant lastFailureTime = null;

        if (execution.isPresent()) {
            ScheduledExecution<Object> exec = execution.get();
            nextRefreshTime = exec.getExecutionTime();
            isCurrentlyRefreshing = exec.isPicked();
            consecutiveFailures = exec.getConsecutiveFailures();

            if (nextRefreshTime != null && !isCurrentlyRefreshing) {
                lastRefreshTime = nextRefreshTime.minusSeconds(definition.intervalSeconds());
            }

            if (consecutiveFailures > 0) {
                lastFailureTime = exec.getLastFailure();
            }
        }

        return ScheduledFeedStatus.of(
                definition.displayName(),
                lastRefreshTime,
                nextRefreshTime,
                isCurrentlyRefreshing,
                consecutiveFailures,
                lastFailureTime,
                definition.intervalSeconds()
        );
    }

    /**
     * Finds the scheduled execution for a task by name.
     *
     * @param taskName The task name to search for
     * @return The scheduled execution if found
     */
    @SuppressWarnings("unchecked")
    protected Optional<ScheduledExecution<Object>> findScheduledExecution(String taskName) {
        return scheduler.getScheduledExecutions()
                .stream()
                .filter(exec -> taskName.equals(exec.getTaskInstance().getTaskName()))
                .map(exec -> (ScheduledExecution<Object>) exec)
                .findFirst();
    }

    /**
     * Returns the list of feed definitions for this provider.
     *
     * <p>Useful for subclasses that need to extend the base behavior.
     *
     * @return Immutable list of feed definitions
     */
    protected List<ScheduledFeedDefinition> getFeedDefinitions() {
        return feedDefinitions;
    }

    private static Map<String, String> buildFeedToTaskMap(List<ScheduledFeedDefinition> definitions) {
        return definitions.stream()
                .collect(Collectors.toMap(
                        ScheduledFeedDefinition::displayName,
                        ScheduledFeedDefinition::taskName
                ));
    }
}
