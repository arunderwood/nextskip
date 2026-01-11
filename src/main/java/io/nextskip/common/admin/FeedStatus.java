package io.nextskip.common.admin;

/**
 * Base interface for feed status information.
 *
 * <p>Sealed to ensure only {@link ScheduledFeedStatus} and
 * {@link SubscriptionFeedStatus} can implement this interface.
 */
public sealed interface FeedStatus permits ScheduledFeedStatus, SubscriptionFeedStatus {

    /**
     * Returns the human-readable name of the feed.
     *
     * @return feed name
     */
    String name();

    /**
     * Returns the type of feed (scheduled or subscription).
     *
     * @return feed type
     */
    FeedType type();

    /**
     * Returns the current health status of the feed.
     *
     * @return health status
     */
    HealthStatus healthStatus();
}
