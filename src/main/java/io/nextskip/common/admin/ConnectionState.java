package io.nextskip.common.admin;

/**
 * Connection state for subscription-based feeds.
 */
public enum ConnectionState {

    /**
     * Active connection, receiving data.
     */
    CONNECTED,

    /**
     * Not connected to the data source.
     */
    DISCONNECTED,

    /**
     * Attempting to reconnect after connection loss.
     */
    RECONNECTING,

    /**
     * Connected but not receiving messages (possible stale connection).
     */
    STALE
}
