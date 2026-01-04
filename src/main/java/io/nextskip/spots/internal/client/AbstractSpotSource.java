package io.nextskip.spots.internal.client;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Abstract base class for spot sources with reconnection support.
 *
 * <p>Provides:
 * <ul>
 *   <li>Automatic reconnection with exponential backoff</li>
 *   <li>Lifecycle management (connect on startup, disconnect on shutdown)</li>
 *   <li>Thread-safe connection state tracking</li>
 *   <li>Callback-based message delivery for Pekko Streams integration</li>
 * </ul>
 *
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #doConnect()} - actual connection logic</li>
 *   <li>{@link #doDisconnect()} - actual disconnection logic</li>
 *   <li>{@link #isConnectedInternal()} - connection status check</li>
 * </ul>
 */
public abstract class AbstractSpotSource implements SpotSource {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSpotSource.class);

    // Backoff configuration
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 60_000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private Consumer<String> messageHandler;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> reconnectFuture;

    protected AbstractSpotSource() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, getSourceName() + "-reconnect");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Performs the actual connection to the data source.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Establish the connection (e.g., MQTT connect)</li>
     *   <li>Subscribe to relevant topics</li>
     *   <li>Set up message handlers that call {@link #emitMessage(String)}</li>
     * </ul>
     *
     * @throws Exception if connection fails
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // Subclasses throw varied exceptions
    protected abstract void doConnect() throws Exception;

    /**
     * Performs the actual disconnection from the data source.
     *
     * <p>Implementations should cleanly close connections and release resources.
     */
    protected abstract void doDisconnect();

    /**
     * Returns the internal connection status.
     *
     * @return true if the underlying connection is established
     */
    protected abstract boolean isConnectedInternal();

    @Override
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Catching all exceptions from subclass
    public void connect() {
        if (!connecting.compareAndSet(false, true)) {
            LOG.debug("{}: Connection already in progress", getSourceName());
            return;
        }

        try {
            LOG.info("{}: Connecting...", getSourceName());
            doConnect();
            consecutiveFailures.set(0);
            LOG.info("{}: Connected successfully", getSourceName());
        } catch (Exception e) {
            LOG.error("{}: Connection failed: {}", getSourceName(), e.getMessage());
            scheduleReconnect();
        } finally {
            connecting.set(false);
        }
    }

    @Override
    @PreDestroy
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Graceful cleanup of any error
    public void disconnect() {
        LOG.info("{}: Disconnecting...", getSourceName());

        // Cancel any pending reconnect
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }

        try {
            doDisconnect();
        } catch (RuntimeException e) {
            LOG.warn("{}: Error during disconnect: {}", getSourceName(), e.getMessage());
        }

        scheduler.shutdown();
        LOG.info("{}: Disconnected", getSourceName());
    }

    @Override
    public boolean isConnected() {
        return isConnectedInternal();
    }

    /**
     * Called when the connection is lost unexpectedly.
     *
     * <p>Subclasses should call this from their connection-lost callbacks
     * to trigger automatic reconnection.
     *
     * <p><b>Note:</b> If the underlying client library handles reconnection
     * internally (e.g., Paho MQTT with {@code setAutomaticReconnect(true)}),
     * subclasses should NOT call this method to avoid dual reconnection.
     *
     * @param cause the reason for disconnection
     */
    protected void onConnectionLost(Throwable cause) {
        LOG.warn("{}: Connection lost: {}", getSourceName(),
                cause != null ? cause.getMessage() : "unknown reason");
        scheduleReconnect();
    }

    /**
     * Emits a message to downstream consumers.
     *
     * <p>Subclasses should call this when a message arrives from the source.
     *
     * @param message the raw message (typically JSON)
     */
    protected void emitMessage(String message) {
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    private void scheduleReconnect() {
        int failures = consecutiveFailures.incrementAndGet();
        long backoffMs = calculateBackoff(failures);

        LOG.info("{}: Scheduling reconnect in {}ms (attempt {})",
                getSourceName(), backoffMs, failures);

        reconnectFuture = scheduler.schedule(this::connect, backoffMs, TimeUnit.MILLISECONDS);
    }

    private long calculateBackoff(int failures) {
        double backoff = INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, failures - 1);
        return Math.min((long) backoff, MAX_BACKOFF_MS);
    }
}
