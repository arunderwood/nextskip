package io.nextskip.common.admin;

/**
 * Type of data feed in the system.
 */
public enum FeedType {

    /**
     * Scheduled/polling feed managed by db-scheduler.
     * Refreshes on a recurring interval.
     */
    SCHEDULED,

    /**
     * Subscription/streaming feed with persistent connection.
     * Receives data in real-time (e.g., MQTT).
     */
    SUBSCRIPTION
}
