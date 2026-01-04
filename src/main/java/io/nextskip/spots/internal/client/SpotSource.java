package io.nextskip.spots.internal.client;

import java.util.function.Consumer;

/**
 * Interface for sources that emit raw spot data.
 *
 * <p>Unlike HTTP-based {@code ExternalDataClient}, SpotSource is designed
 * for push-based streaming protocols like MQTT. It provides:
 * <ul>
 *   <li>Push-based message delivery via callback</li>
 *   <li>Connection lifecycle management</li>
 *   <li>Connection status monitoring</li>
 * </ul>
 *
 * <p>Implementations should handle reconnection internally and emit data
 * as raw strings (typically JSON) for downstream parsing.
 *
 * @see PskReporterMqttSource
 */
public interface SpotSource {

    /**
     * Sets the handler that receives raw spot data.
     *
     * <p>The handler will be called for each message received from the source.
     * This must be called before {@link #connect()}.
     *
     * @param handler consumer that processes raw spot data (typically JSON strings)
     */
    void setMessageHandler(Consumer<String> handler);

    /**
     * Initiates connection to the data source.
     *
     * <p>This method should be idempotent - calling it multiple times
     * should not create multiple connections.
     */
    void connect();

    /**
     * Disconnects from the data source.
     *
     * <p>Should cleanly close any open connections and release resources.
     */
    void disconnect();

    /**
     * Returns whether the source is currently connected.
     *
     * @return true if connected and receiving data
     */
    boolean isConnected();

    /**
     * Returns a human-readable name for this source.
     *
     * @return source name (e.g., "PSKReporter MQTT")
     */
    String getSourceName();
}
