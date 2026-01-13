package io.nextskip.admin.model;

/**
 * Categorizes feeds by their data refresh mechanism.
 *
 * <p>This distinction affects how status is displayed and what actions are available:
 * <ul>
 *   <li>{@link #SCHEDULED} - Polled feeds using db-scheduler, show last execution time</li>
 *   <li>{@link #SUBSCRIPTION} - Persistent connections (MQTT/WebSocket), show connection status</li>
 * </ul>
 */
public enum FeedType {

    /**
     * Scheduled feeds that poll external APIs on a fixed interval.
     *
     * <p>Examples: NOAA solar indices, HamQSL band conditions, POTA/SOTA activations.
     * These support manual refresh triggers.
     */
    SCHEDULED,

    /**
     * Subscription-based feeds with persistent connections.
     *
     * <p>Examples: PSKReporter MQTT.
     * These show connection status but don't support manual refresh.
     */
    SUBSCRIPTION
}
