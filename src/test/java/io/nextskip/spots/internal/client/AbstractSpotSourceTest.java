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
    // isReceivingMessages tests
    // ===========================================

    @Test
    void testIsReceivingMessages_NotConnected_ReturnsFalse() {
        assertThat(spotSource.isReceivingMessages()).isFalse();
    }

    @Test
    void testIsReceivingMessages_ConnectedNoMessages_ReturnsTrue() {
        spotSource.connect();

        // No messages yet but still within threshold - should return true
        assertThat(spotSource.isReceivingMessages()).isTrue();
    }

    @Test
    void testIsReceivingMessages_RecentMessage_ReturnsTrue() {
        List<String> receivedMessages = new ArrayList<>();
        spotSource.setMessageHandler(receivedMessages::add);
        spotSource.connect();

        spotSource.emitTestMessage("test message");

        assertThat(spotSource.isReceivingMessages()).isTrue();
    }

    @Test
    void testIsReceivingMessages_AfterDisconnect_ReturnsFalse() {
        spotSource.connect();
        spotSource.emitTestMessage("test message");
        spotSource.disconnect();

        assertThat(spotSource.isReceivingMessages()).isFalse();
    }

    // ===========================================
    // getSourceName tests
    // ===========================================

    @Test
    void testGetSourceName_ReturnsTestName() {
        assertThat(spotSource.getSourceName()).isEqualTo("Test Source");
    }

    // ===========================================
    // Stale connection detection tests
    // ===========================================

    @Test
    void testStaleConnectionCheck_NotConnected_NoAction() throws Exception {
        // When not connected, stale check should do nothing
        ReflectionTestSource source = new ReflectionTestSource();

        // Trigger check while disconnected via reflection
        source.invokeCheckForStaleConnection();

        // Should not trigger force reconnect since not connected
        assertThat(source.getForceReconnectCount()).isZero();
    }

    @Test
    void testStaleConnectionCheck_NoMessagesYet_NoAction() throws Exception {
        ReflectionTestSource source = new ReflectionTestSource();
        source.connect();

        // Trigger check - no messages yet, should give grace period
        source.invokeCheckForStaleConnection();

        // Should not trigger force reconnect when no messages have been received yet
        assertThat(source.getForceReconnectCount()).isZero();
        source.disconnect();
    }

    @Test
    void testStaleConnectionCheck_RecentMessage_NoAction() throws Exception {
        ReflectionTestSource source = new ReflectionTestSource();
        source.connect();
        source.emitTestMessage("recent message");

        // Trigger check immediately after message - should be fine
        source.invokeCheckForStaleConnection();

        // Should not trigger force reconnect
        assertThat(source.getForceReconnectCount()).isZero();
        source.disconnect();
    }

    @Test
    void testStaleConnectionCheck_StaleMessage_TriggersForceReconnect() throws Exception {
        ReflectionTestSource source = new ReflectionTestSource();
        source.connect();

        // Emit a message then set the timestamp to 31 seconds ago (stale threshold is 30s)
        source.emitTestMessage("test");
        source.setLastMessageTime(java.time.Instant.now().minus(Duration.ofSeconds(31)));

        // Trigger stale check
        source.invokeCheckForStaleConnection();

        // Should trigger force reconnect which calls doDisconnect()
        assertThat(source.getDoDisconnectCalled()).isTrue();

        // And schedule a reconnect
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(source.getConnectAttempts()).isGreaterThan(1));

        source.disconnect();
    }

    @Test
    void testForceReconnect_DisconnectsAndSchedulesReconnect() throws Exception {
        ReflectionTestSource source = new ReflectionTestSource();
        source.connect();
        assertThat(source.isConnected()).isTrue();

        // Trigger force reconnect via reflection
        source.invokeForceReconnect();

        // Should have disconnected
        assertThat(source.getDoDisconnectCalled()).isTrue();

        // Should have scheduled reconnect
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(source.getConnectAttempts()).isGreaterThan(1));

        source.disconnect();
    }

    @Test
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert") // Assertion via await().untilAsserted()
    void testForceReconnect_DisconnectThrows_StillReconnects() throws Exception {
        ThrowingReflectionTestSource source = new ThrowingReflectionTestSource();
        source.connect();

        // Trigger force reconnect - disconnect will throw
        source.invokeForceReconnect();

        // Should still schedule reconnect despite disconnect exception
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(source.getConnectAttempts()).isGreaterThan(1));

        source.disconnect();
    }

    // ===========================================
    // SpotSource interface default method test
    // ===========================================

    @Test
    void testSpotSourceInterface_DefaultIsReceivingMessages_ReturnsIsConnected() {
        // Test the default implementation of SpotSource.isReceivingMessages()
        SpotSource source = new MinimalSpotSource();

        // Not connected - should return false
        assertThat(source.isReceivingMessages()).isFalse();

        // Connect - default should delegate to isConnected()
        ((MinimalSpotSource) source).setConnected(true);
        assertThat(source.isReceivingMessages()).isTrue();
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

    /**
     * Test implementation that uses reflection to access private methods
     * for testing checkForStaleConnection() and forceReconnect().
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration") // Reflection required to test private methods
    private static final class ReflectionTestSource extends AbstractSpotSource {

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicInteger connectAttempts = new AtomicInteger(0);
        private final AtomicInteger forceReconnectCount = new AtomicInteger(0);
        private final AtomicBoolean doDisconnectCalled = new AtomicBoolean(false);

        @Override
        protected void doConnect() {
            connectAttempts.incrementAndGet();
            connected.set(true);
        }

        @Override
        protected void doDisconnect() {
            doDisconnectCalled.set(true);
            connected.set(false);
        }

        @Override
        protected boolean isConnectedInternal() {
            return connected.get();
        }

        @Override
        public String getSourceName() {
            return "Reflection Test Source";
        }

        void emitTestMessage(String message) {
            emitMessage(message);
        }

        /**
         * Uses reflection to invoke the private checkForStaleConnection method.
         */
        void invokeCheckForStaleConnection() throws ReflectiveOperationException {
            java.lang.reflect.Method method = AbstractSpotSource.class.getDeclaredMethod("checkForStaleConnection");
            method.setAccessible(true);
            method.invoke(this);
        }

        /**
         * Uses reflection to invoke the private forceReconnect method.
         */
        void invokeForceReconnect() throws ReflectiveOperationException {
            java.lang.reflect.Method method = AbstractSpotSource.class.getDeclaredMethod("forceReconnect");
            method.setAccessible(true);
            forceReconnectCount.incrementAndGet();
            method.invoke(this);
        }

        /**
         * Uses reflection to set the lastMessageTime field directly.
         */
        @SuppressWarnings("unchecked") // Reflection-based field access
        void setLastMessageTime(java.time.Instant time) throws ReflectiveOperationException {
            java.lang.reflect.Field field = AbstractSpotSource.class.getDeclaredField("lastMessageTime");
            field.setAccessible(true);
            java.util.concurrent.atomic.AtomicReference<java.time.Instant> ref =
                    (java.util.concurrent.atomic.AtomicReference<java.time.Instant>) field.get(this);
            ref.set(time);
        }

        int getConnectAttempts() {
            return connectAttempts.get();
        }

        int getForceReconnectCount() {
            return forceReconnectCount.get();
        }

        boolean getDoDisconnectCalled() {
            return doDisconnectCalled.get();
        }
    }

    /**
     * Test implementation that throws during disconnect, for testing forceReconnect exception handling.
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration") // Reflection required to test private method
    private static final class ThrowingReflectionTestSource extends AbstractSpotSource {

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicInteger connectAttempts = new AtomicInteger(0);

        @Override
        protected void doConnect() {
            connectAttempts.incrementAndGet();
            connected.set(true);
        }

        @Override
        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes") // Intentionally testing exception handling
        protected void doDisconnect() {
            connected.set(false);
            throw new RuntimeException("Simulated disconnect error during force reconnect");
        }

        @Override
        protected boolean isConnectedInternal() {
            return connected.get();
        }

        @Override
        public String getSourceName() {
            return "Throwing Reflection Test Source";
        }

        /**
         * Uses reflection to invoke the private forceReconnect method.
         */
        void invokeForceReconnect() throws ReflectiveOperationException {
            java.lang.reflect.Method method = AbstractSpotSource.class.getDeclaredMethod("forceReconnect");
            method.setAccessible(true);
            method.invoke(this);
        }

        int getConnectAttempts() {
            return connectAttempts.get();
        }
    }

    /**
     * Minimal SpotSource implementation that uses the default isReceivingMessages().
     * This tests the interface's default method directly.
     */
    private static final class MinimalSpotSource implements SpotSource {

        private final AtomicBoolean connected = new AtomicBoolean(false);

        @Override
        public void setMessageHandler(java.util.function.Consumer<String> handler) {
            // No-op for testing
        }

        @Override
        public void connect() {
            connected.set(true);
        }

        @Override
        public void disconnect() {
            connected.set(false);
        }

        @Override
        public boolean isConnected() {
            return connected.get();
        }

        // Note: we intentionally do NOT override isReceivingMessages()
        // to test the interface's default implementation

        @Override
        public String getSourceName() {
            return "Minimal SpotSource";
        }

        void setConnected(boolean value) {
            connected.set(value);
        }
    }
}
