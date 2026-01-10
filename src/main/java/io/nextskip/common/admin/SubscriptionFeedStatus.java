package io.nextskip.common.admin;

import java.time.Instant;

/**
 * Status information for subscription/streaming feeds (e.g., MQTT).
 *
 * @param name Human-readable feed name
 * @param type Always {@link FeedType#SUBSCRIPTION}
 * @param healthStatus Current health status
 * @param connectionState Current connection state
 * @param lastMessageTime Timestamp of last received message (null if none)
 * @param consecutiveReconnectAttempts Number of consecutive reconnect attempts
 */
public record SubscriptionFeedStatus(
        String name,
        FeedType type,
        HealthStatus healthStatus,
        ConnectionState connectionState,
        Instant lastMessageTime,
        int consecutiveReconnectAttempts
) implements FeedStatus {

    /**
     * Creates a SubscriptionFeedStatus with automatically calculated health status.
     *
     * @param name Human-readable feed name
     * @param connectionState Current connection state
     * @param lastMessageTime Timestamp of last received message
     * @param consecutiveReconnectAttempts Number of consecutive reconnect attempts
     * @return SubscriptionFeedStatus with calculated health status
     */
    public static SubscriptionFeedStatus of(
            String name,
            ConnectionState connectionState,
            Instant lastMessageTime,
            int consecutiveReconnectAttempts) {

        HealthStatus healthStatus = calculateHealthStatus(connectionState);

        return new SubscriptionFeedStatus(
                name,
                FeedType.SUBSCRIPTION,
                healthStatus,
                connectionState,
                lastMessageTime,
                consecutiveReconnectAttempts
        );
    }

    private static HealthStatus calculateHealthStatus(ConnectionState connectionState) {
        return switch (connectionState) {
            case CONNECTED -> HealthStatus.HEALTHY;
            case STALE, RECONNECTING -> HealthStatus.DEGRADED;
            case DISCONNECTED -> HealthStatus.UNHEALTHY;
        };
    }
}
