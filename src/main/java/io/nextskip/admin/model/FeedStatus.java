package io.nextskip.admin.model;

import java.time.Instant;

/**
 * Represents the current status of a data feed for the admin UI.
 *
 * <p>This record provides a unified view of feed status regardless of
 * whether the feed is scheduled (polled) or subscription-based (persistent).
 *
 * @param id unique identifier for the feed (task name or subscription ID)
 * @param displayName human-readable name for the admin UI
 * @param type whether this is a scheduled or subscription feed
 * @param healthy whether the feed is operating normally
 * @param lastActivity timestamp of last successful data refresh or message received
 * @param statusMessage optional descriptive status (e.g., "Connected", "Last run 2m ago")
 */
public record FeedStatus(
        String id,
        String displayName,
        FeedType type,
        boolean healthy,
        Instant lastActivity,
        String statusMessage
) {

    /**
     * Creates a FeedStatus for a healthy scheduled feed.
     *
     * @param id the task name identifier
     * @param displayName human-readable display name
     * @param lastExecution timestamp of last execution
     * @return a healthy scheduled feed status
     */
    public static FeedStatus scheduledHealthy(String id, String displayName, Instant lastExecution) {
        return new FeedStatus(id, displayName, FeedType.SCHEDULED, true, lastExecution, null);
    }

    /**
     * Creates a FeedStatus for an unhealthy scheduled feed.
     *
     * @param id the task name identifier
     * @param displayName human-readable display name
     * @param lastExecution timestamp of last execution (may be null)
     * @param message error or warning message
     * @return an unhealthy scheduled feed status
     */
    public static FeedStatus scheduledUnhealthy(
            String id, String displayName, Instant lastExecution, String message) {
        return new FeedStatus(id, displayName, FeedType.SCHEDULED, false, lastExecution, message);
    }

    /**
     * Creates a FeedStatus for a connected subscription feed.
     *
     * @param id the subscription identifier
     * @param displayName human-readable display name
     * @param lastMessage timestamp of last message received
     * @return a connected subscription feed status
     */
    public static FeedStatus subscriptionConnected(String id, String displayName, Instant lastMessage) {
        return new FeedStatus(id, displayName, FeedType.SUBSCRIPTION, true, lastMessage, "Connected");
    }

    /**
     * Creates a FeedStatus for a disconnected subscription feed.
     *
     * @param id the subscription identifier
     * @param displayName human-readable display name
     * @param lastMessage timestamp of last message received (may be null)
     * @return a disconnected subscription feed status
     */
    public static FeedStatus subscriptionDisconnected(String id, String displayName, Instant lastMessage) {
        return new FeedStatus(id, displayName, FeedType.SUBSCRIPTION, false, lastMessage, "Disconnected");
    }
}
