package io.nextskip.common.api;

import java.time.Instant;

/**
 * Provides connection status information for subscription-based data feeds.
 *
 * <p>Subscription feeds maintain persistent connections (e.g., MQTT, WebSocket)
 * rather than polling on a schedule. This interface exposes their connection state
 * for the admin Feed Manager UI.
 *
 * <p>This interface follows the Open-Closed Principle: adding a new subscription feed
 * requires only implementing this interface, with no changes to admin module code.
 *
 * @see io.nextskip.admin.internal.FeedStatusService
 */
public interface SubscriptionStatusProvider {

    /**
     * Returns a unique identifier for this subscription.
     *
     * @return the subscription ID (e.g., "pskreporter-mqtt")
     */
    String getSubscriptionId();

    /**
     * Returns a human-readable display name for the admin UI.
     *
     * @return the display name (e.g., "PSKReporter MQTT")
     */
    String getDisplayName();

    /**
     * Checks if the subscription is currently connected and receiving data.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Returns the timestamp of the last message received.
     *
     * @return the last message time, or null if no messages have been received
     */
    Instant getLastMessageTime();
}
