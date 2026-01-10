package io.nextskip.common.admin;

/**
 * Defines a scheduled feed for admin status tracking.
 *
 * <p>Encapsulates the configuration needed to monitor a scheduled feed:
 * <ul>
 *   <li>displayName - Human-readable name shown in the admin UI</li>
 *   <li>taskName - The db-scheduler task name (must match RecurringTask config)</li>
 *   <li>intervalSeconds - Expected refresh interval for "last refresh" calculation</li>
 * </ul>
 *
 * @param displayName The name displayed in the admin UI
 * @param taskName The db-scheduler task name
 * @param intervalSeconds The expected refresh interval in seconds
 */
public record ScheduledFeedDefinition(
        String displayName,
        String taskName,
        long intervalSeconds
) {

    /**
     * Creates a ScheduledFeedDefinition with validation.
     *
     * @param displayName The name displayed in the admin UI
     * @param taskName The db-scheduler task name
     * @param intervalSeconds The expected refresh interval in seconds
     */
    public ScheduledFeedDefinition {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be null or blank");
        }
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("taskName cannot be null or blank");
        }
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be positive");
        }
    }
}
