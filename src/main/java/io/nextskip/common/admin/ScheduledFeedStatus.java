package io.nextskip.common.admin;

import java.time.Instant;

/**
 * Status information for scheduled/polling feeds managed by db-scheduler.
 *
 * @param name Human-readable feed name
 * @param type Always {@link FeedType#SCHEDULED}
 * @param healthStatus Current health status
 * @param lastRefreshTime Timestamp of last successful refresh (null if never)
 * @param nextRefreshTime Timestamp of next scheduled refresh (null if unknown)
 * @param isCurrentlyRefreshing Whether refresh is in progress
 * @param consecutiveFailures Number of consecutive failed refresh attempts
 * @param lastFailureTime Timestamp of last failed attempt (null if none)
 * @param refreshIntervalSeconds Normal refresh interval in seconds
 */
public record ScheduledFeedStatus(
        String name,
        FeedType type,
        HealthStatus healthStatus,
        Instant lastRefreshTime,
        Instant nextRefreshTime,
        boolean isCurrentlyRefreshing,
        int consecutiveFailures,
        Instant lastFailureTime,
        long refreshIntervalSeconds
) implements FeedStatus {

    private static final int DEGRADED_THRESHOLD = 1;
    private static final int UNHEALTHY_THRESHOLD = 3;

    /**
     * Creates a ScheduledFeedStatus with automatically calculated health status.
     *
     * @param name Human-readable feed name
     * @param lastRefreshTime Timestamp of last successful refresh
     * @param nextRefreshTime Timestamp of next scheduled refresh
     * @param isCurrentlyRefreshing Whether refresh is in progress
     * @param consecutiveFailures Number of consecutive failed refresh attempts
     * @param lastFailureTime Timestamp of last failed attempt
     * @param refreshIntervalSeconds Normal refresh interval in seconds
     * @return ScheduledFeedStatus with calculated health status
     */
    public static ScheduledFeedStatus of(
            String name,
            Instant lastRefreshTime,
            Instant nextRefreshTime,
            boolean isCurrentlyRefreshing,
            int consecutiveFailures,
            Instant lastFailureTime,
            long refreshIntervalSeconds) {

        HealthStatus healthStatus = calculateHealthStatus(consecutiveFailures);

        return new ScheduledFeedStatus(
                name,
                FeedType.SCHEDULED,
                healthStatus,
                lastRefreshTime,
                nextRefreshTime,
                isCurrentlyRefreshing,
                consecutiveFailures,
                lastFailureTime,
                refreshIntervalSeconds
        );
    }

    private static HealthStatus calculateHealthStatus(int consecutiveFailures) {
        if (consecutiveFailures >= UNHEALTHY_THRESHOLD) {
            return HealthStatus.UNHEALTHY;
        } else if (consecutiveFailures >= DEGRADED_THRESHOLD) {
            return HealthStatus.DEGRADED;
        }
        return HealthStatus.HEALTHY;
    }
}
