package io.nextskip.admin.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FeedStatus record and factory methods.
 */
class FeedStatusTest {

    private static final String TEST_ID = "test-feed";
    private static final String TEST_NAME = "Test Feed";
    private static final Instant TEST_TIME = Instant.parse("2024-01-01T12:00:00Z");

    @Test
    void testScheduledHealthy_CreatesCorrectStatus() {
        FeedStatus status = FeedStatus.scheduledHealthy(TEST_ID, TEST_NAME, TEST_TIME);

        assertThat(status.id()).isEqualTo(TEST_ID);
        assertThat(status.displayName()).isEqualTo(TEST_NAME);
        assertThat(status.type()).isEqualTo(FeedType.SCHEDULED);
        assertThat(status.healthy()).isTrue();
        assertThat(status.lastActivity()).isEqualTo(TEST_TIME);
        assertThat(status.statusMessage()).isNull(); // Healthy feeds have no message
    }

    @Test
    void testScheduledUnhealthy_CreatesCorrectStatus() {
        FeedStatus status = FeedStatus.scheduledUnhealthy(TEST_ID, TEST_NAME, TEST_TIME, "Error occurred");

        assertThat(status.id()).isEqualTo(TEST_ID);
        assertThat(status.displayName()).isEqualTo(TEST_NAME);
        assertThat(status.type()).isEqualTo(FeedType.SCHEDULED);
        assertThat(status.healthy()).isFalse();
        assertThat(status.lastActivity()).isEqualTo(TEST_TIME);
        assertThat(status.statusMessage()).isEqualTo("Error occurred");
    }

    @Test
    void testScheduledUnhealthy_WithNullLastActivity() {
        FeedStatus status = FeedStatus.scheduledUnhealthy(TEST_ID, TEST_NAME, null, "Never executed");

        assertThat(status.lastActivity()).isNull();
        assertThat(status.healthy()).isFalse();
    }

    @Test
    void testSubscriptionConnected_CreatesCorrectStatus() {
        FeedStatus status = FeedStatus.subscriptionConnected(TEST_ID, TEST_NAME, TEST_TIME);

        assertThat(status.id()).isEqualTo(TEST_ID);
        assertThat(status.displayName()).isEqualTo(TEST_NAME);
        assertThat(status.type()).isEqualTo(FeedType.SUBSCRIPTION);
        assertThat(status.healthy()).isTrue();
        assertThat(status.lastActivity()).isEqualTo(TEST_TIME);
        assertThat(status.statusMessage()).isEqualTo("Connected");
    }

    @Test
    void testSubscriptionConnected_WithNullLastActivity() {
        FeedStatus status = FeedStatus.subscriptionConnected(TEST_ID, TEST_NAME, null);

        assertThat(status.lastActivity()).isNull();
        assertThat(status.healthy()).isTrue();
    }

    @Test
    void testSubscriptionDisconnected_CreatesCorrectStatus() {
        FeedStatus status = FeedStatus.subscriptionDisconnected(TEST_ID, TEST_NAME, TEST_TIME);

        assertThat(status.id()).isEqualTo(TEST_ID);
        assertThat(status.displayName()).isEqualTo(TEST_NAME);
        assertThat(status.type()).isEqualTo(FeedType.SUBSCRIPTION);
        assertThat(status.healthy()).isFalse();
        assertThat(status.lastActivity()).isEqualTo(TEST_TIME);
        assertThat(status.statusMessage()).isEqualTo("Disconnected");
    }

    @Test
    void testSubscriptionDisconnected_WithNullLastActivity() {
        FeedStatus status = FeedStatus.subscriptionDisconnected(TEST_ID, TEST_NAME, null);

        assertThat(status.lastActivity()).isNull();
        assertThat(status.healthy()).isFalse();
    }
}
