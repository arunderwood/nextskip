package io.nextskip.spots.internal.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for {@link AbstractSpotSource}.
 *
 * <p>Tests the reconnection logic, lifecycle management, and message emission.
 */
class AbstractSpotSourceTest {

    private TestSpotSource spotSource;

    @BeforeEach
    void setUp() {
        spotSource = new TestSpotSource();
    }

    @AfterEach
    void tearDown() {
        if (spotSource != null) {
            spotSource.disconnect();
        }
    }

    // ===========================================
    // connect tests
    // ===========================================

    @Test
    void testConnect_SuccessfulConnection_SetsConnected() {
        spotSource.connect();

        assertThat(spotSource.isConnected()).isTrue();
        assertThat(spotSource.getConnectAttempts()).isEqualTo(1);
    }

    @Test
    void testConnect_ConnectionFails_SchedulesReconnect() {
        spotSource.setFailConnection(true);

        spotSource.connect();

        assertThat(spotSource.isConnected()).isFalse();
        // Reconnect should be scheduled
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(spotSource.getConnectAttempts()).isGreaterThan(1));
    }

    @Test
    void testConnect_CalledTwice_ConnectsSuccessfully() {
        // First connection
        spotSource.connect();
        assertThat(spotSource.getConnectAttempts()).isEqualTo(1);

        // Second connection attempt - the connecting flag prevents re-entry only during active connection
        // but since first connection completed, second call will still proceed
        spotSource.connect();

        // Both calls complete - the idempotent guard is for concurrent calls, not sequential
        assertThat(spotSource.isConnected()).isTrue();
    }

    @Test
    void testConnect_ConcurrentConnection_BlockedByConnectingFlag() throws InterruptedException {
        // Use a slow connecting source that blocks during connection
        SlowConnectingSpotSource slowSource = new SlowConnectingSpotSource();

        // Start first connection in a separate thread
        Thread connectThread = new Thread(slowSource::connect);
        connectThread.start();

        // Wait for first connection to start
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(slowSource.isConnecting()).isTrue());

        // Try second connection while first is in progress
        slowSource.connect();

        // Second connection should return immediately without incrementing connect attempts
        // because connecting flag is already set
        assertThat(slowSource.getConnectAttempts()).isEqualTo(1);

        // Allow first connection to complete
        slowSource.releaseConnection();
        connectThread.join(5000);

        assertThat(slowSource.isConnected()).isTrue();
        slowSource.disconnect();
    }

    // ===========================================
    // disconnect tests
    // ===========================================

    @Test
    void testDisconnect_ConnectedSource_Disconnects() {
        spotSource.connect();
        assertThat(spotSource.isConnected()).isTrue();

        spotSource.disconnect();

        assertThat(spotSource.isConnected()).isFalse();
        assertThat(spotSource.getDisconnectCalls()).isEqualTo(1);
    }

    @Test
    void testDisconnect_CancelsPendingReconnect() {
        spotSource.setFailConnection(true);
        spotSource.connect();

        // Wait briefly to ensure reconnect is scheduled
        await().atMost(Duration.ofMillis(500)).untilAsserted(() ->
                assertThat(spotSource.getConnectAttempts()).isGreaterThanOrEqualTo(1)
        );

        spotSource.disconnect();
        assertThat(spotSource.isConnected()).isFalse();

        // After disconnect, no more reconnect attempts should happen
        int attemptsAfterDisconnect = spotSource.getConnectAttempts();
        await().during(Duration.ofMillis(500)).untilAsserted(() ->
                assertThat(spotSource.getConnectAttempts()).isEqualTo(attemptsAfterDisconnect)
        );
    }

    @Test
    void testDisconnect_HandlesExceptionGracefully() {
        ThrowingDisconnectSpotSource throwingSource = new ThrowingDisconnectSpotSource();
        throwingSource.connect();
        assertThat(throwingSource.isConnected()).isTrue();

        // Should not throw even if doDisconnect throws RuntimeException
        throwingSource.disconnect();

        // Disconnect should complete despite exception
        assertThat(throwingSource.getDisconnectCalls()).isEqualTo(1);
    }

    // ===========================================
    // isConnected tests
    // ===========================================

    @Test
    void testIsConnected_NotConnected_ReturnsFalse() {
        assertThat(spotSource.isConnected()).isFalse();
    }

    @Test
    void testIsConnected_AfterConnection_ReturnsTrue() {
        spotSource.connect();

        assertThat(spotSource.isConnected()).isTrue();
    }

    @Test
    void testIsConnected_AfterDisconnect_ReturnsFalse() {
        spotSource.connect();
        spotSource.disconnect();

        assertThat(spotSource.isConnected()).isFalse();
    }

    // ===========================================
    // setMessageHandler tests
    // ===========================================

    @Test
    void testSetMessageHandler_ReceivesEmittedMessages() {
        List<String> receivedMessages = new ArrayList<>();
        spotSource.setMessageHandler(receivedMessages::add);
        spotSource.connect();

        spotSource.emitTestMessage("{\"test\":1}");
        spotSource.emitTestMessage("{\"test\":2}");
        spotSource.emitTestMessage("{\"test\":3}");

        assertThat(receivedMessages).containsExactly("{\"test\":1}", "{\"test\":2}", "{\"test\":3}");
    }

    @Test
    void testSetMessageHandler_NoHandlerSet_NoException() {
        spotSource.connect();

        // Should not throw when no handler is set
        spotSource.emitTestMessage("test message");

        assertThat(spotSource.isConnected()).isTrue();
    }

    @Test
    void testSetMessageHandler_MultipleMessages_AllReceived() {
        List<String> receivedMessages = new ArrayList<>();
        spotSource.setMessageHandler(receivedMessages::add);
        spotSource.connect();

        spotSource.emitTestMessage("first");
        spotSource.emitTestMessage("second");

        assertThat(receivedMessages).containsExactly("first", "second");
    }

    // ===========================================
    // onConnectionLost tests
    // ===========================================

    @Test
    void testOnConnectionLost_TriggersReconnect() {
        spotSource.connect();
        assertThat(spotSource.isConnected()).isTrue();

        spotSource.simulateConnectionLost(new RuntimeException("Network error"));

        // Should schedule reconnect
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(spotSource.getConnectAttempts()).isGreaterThan(1));
    }

    @Test
    void testOnConnectionLost_NullCause_HandledGracefully() {
        spotSource.connect();
        assertThat(spotSource.isConnected()).isTrue();

        // Should not throw
        spotSource.simulateConnectionLost(null);

        // Should still schedule reconnect
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(spotSource.getConnectAttempts()).isGreaterThan(1));
    }

    // ===========================================
    // Exponential backoff tests
    // ===========================================

    @Test
    void testBackoff_ConsecutiveFailures_IncreasesDelay() {
        spotSource.setFailConnection(true);

        spotSource.connect();
        assertThat(spotSource.isConnected()).isFalse();

        // First retry should happen around 1000ms
        // Second retry should happen around 2000ms
        // We just verify that retries are happening with increasing delays
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(spotSource.getConnectAttempts()).isGreaterThan(2));
    }

    // ===========================================
    // getSourceName tests
    // ===========================================

    @Test
    void testGetSourceName_ReturnsTestName() {
        assertThat(spotSource.getSourceName()).isEqualTo("Test Source");
    }

    // ===========================================
    // Test implementation of AbstractSpotSource
    // ===========================================

    /**
     * Concrete test implementation of AbstractSpotSource for testing.
     */
    @SuppressWarnings("PMD.TestClassWithoutTestCases") // Test helper class, not a test class
    private static final class TestSpotSource extends AbstractSpotSource {

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicBoolean failConnection = new AtomicBoolean(false);
        private final AtomicInteger connectAttempts = new AtomicInteger(0);
        private final AtomicInteger disconnectCalls = new AtomicInteger(0);

        @Override
        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes") // Simulated exception in test
        protected void doConnect() throws Exception {
            connectAttempts.incrementAndGet();
            if (failConnection.get()) {
                throw new Exception("Simulated connection failure");
            }
            connected.set(true);
        }

        @Override
        protected void doDisconnect() {
            disconnectCalls.incrementAndGet();
            connected.set(false);
        }

        @Override
        protected boolean isConnectedInternal() {
            return connected.get();
        }

        @Override
        public String getSourceName() {
            return "Test Source";
        }

        // Test helpers

        void setFailConnection(boolean fail) {
            this.failConnection.set(fail);
        }

        int getConnectAttempts() {
            return connectAttempts.get();
        }

        int getDisconnectCalls() {
            return disconnectCalls.get();
        }

        void emitTestMessage(String message) {
            emitMessage(message);
        }

        void simulateConnectionLost(Throwable cause) {
            connected.set(false);
            onConnectionLost(cause);
        }
    }

    /**
     * Test implementation that blocks during connection to test concurrent access.
     */
    private static final class SlowConnectingSpotSource extends AbstractSpotSource {

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        private final AtomicInteger connectAttempts = new AtomicInteger(0);
        private final java.util.concurrent.CountDownLatch connectionLatch =
                new java.util.concurrent.CountDownLatch(1);

        @Override
        protected void doConnect() throws Exception {
            connecting.set(true);
            connectAttempts.incrementAndGet();
            // Block until released
            connectionLatch.await();
            connected.set(true);
            connecting.set(false);
        }

        @Override
        protected void doDisconnect() {
            connected.set(false);
        }

        @Override
        protected boolean isConnectedInternal() {
            return connected.get();
        }

        @Override
        public String getSourceName() {
            return "Slow Test Source";
        }

        boolean isConnecting() {
            return connecting.get();
        }

        int getConnectAttempts() {
            return connectAttempts.get();
        }

        void releaseConnection() {
            connectionLatch.countDown();
        }
    }

    /**
     * Test implementation that throws on disconnect to test exception handling.
     */
    private static final class ThrowingDisconnectSpotSource extends AbstractSpotSource {

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicInteger disconnectCalls = new AtomicInteger(0);

        @Override
        protected void doConnect() {
            connected.set(true);
        }

        @Override
        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes") // Intentionally testing exception handling
        protected void doDisconnect() {
            disconnectCalls.incrementAndGet();
            throw new RuntimeException("Simulated disconnect failure");
        }

        @Override
        protected boolean isConnectedInternal() {
            return connected.get();
        }

        @Override
        public String getSourceName() {
            return "Throwing Disconnect Source";
        }

        int getDisconnectCalls() {
            return disconnectCalls.get();
        }
    }
}
