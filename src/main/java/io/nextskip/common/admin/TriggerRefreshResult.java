package io.nextskip.common.admin;

import java.time.Instant;

/**
 * Result of triggering a manual feed refresh.
 *
 * @param success Whether the refresh was triggered successfully
 * @param message Human-readable result message
 * @param feedName Name of the feed that was triggered
 * @param scheduledFor When the refresh is scheduled (null if not triggered)
 */
public record TriggerRefreshResult(
        boolean success,
        String message,
        String feedName,
        Instant scheduledFor
) {

    /**
     * Creates a successful trigger result.
     *
     * @param feedName Name of the feed
     * @param scheduledFor When the refresh is scheduled
     * @return Success result
     */
    public static TriggerRefreshResult success(String feedName, Instant scheduledFor) {
        return new TriggerRefreshResult(
                true,
                "Refresh triggered successfully",
                feedName,
                scheduledFor
        );
    }

    /**
     * Creates a failure result for when a feed is already refreshing.
     *
     * @param feedName Name of the feed
     * @return Failure result
     */
    public static TriggerRefreshResult alreadyRefreshing(String feedName) {
        return new TriggerRefreshResult(
                false,
                "Feed is already refreshing",
                feedName,
                null
        );
    }

    /**
     * Creates a failure result for an unknown feed.
     *
     * @param feedName Name of the feed
     * @return Failure result
     */
    public static TriggerRefreshResult unknownFeed(String feedName) {
        return new TriggerRefreshResult(
                false,
                "Unknown feed: " + feedName,
                feedName,
                null
        );
    }

    /**
     * Creates a failure result for when refresh cannot be triggered on subscription feeds.
     *
     * @param feedName Name of the feed
     * @return Failure result
     */
    public static TriggerRefreshResult notScheduledFeed(String feedName) {
        return new TriggerRefreshResult(
                false,
                "Cannot trigger refresh for subscription feeds",
                feedName,
                null
        );
    }
}
