package io.nextskip.spots.internal.admin;

import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import io.nextskip.common.admin.ConnectionState;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.FeedType;
import io.nextskip.common.admin.HealthStatus;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.SubscriptionFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
import io.nextskip.spots.internal.client.SpotSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static io.nextskip.common.admin.AdminProviderTestFixtures.createMockExecution;
import static io.nextskip.test.TestConstants.FEED_BAND_ACTIVITY;
import static io.nextskip.test.TestConstants.FEED_PSKREPORTER_MQTT;
import static io.nextskip.test.TestConstants.MODULE_SPOTS;
import static io.nextskip.test.TestConstants.TASK_BAND_ACTIVITY_REFRESH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpotsAdminProvider}.
 *
 * <p>Tests feed status retrieval and trigger refresh functionality
 * for spots module feeds (PSKReporter MQTT subscription, Band Activity scheduled).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotsAdminProvider")
class SpotsAdminProviderTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private SpotSource spotSource;

    private SpotsAdminProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SpotsAdminProvider(scheduler, spotSource);
    }

    @Test
    @DisplayName("returns 'spots' as module name")
    void testGetModuleName_ReturnsCorrectName() {
        assertEquals(MODULE_SPOTS, provider.getModuleName());
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns two feeds for spots module")
        void testGetFeedStatuses_ReturnsTwoFeeds() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();

            // Then
            assertEquals(2, statuses.size());
        }

        @Test
        @DisplayName("returns PSKReporter MQTT as subscription feed")
        void testGetFeedStatuses_PskReporterFeed_IsSubscriptionType() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            FeedStatus mqttFeed = statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SUBSCRIPTION, mqttFeed.type());
            assertTrue(mqttFeed instanceof SubscriptionFeedStatus);
        }

        @Test
        @DisplayName("returns Band Activity as scheduled feed")
        void testGetFeedStatuses_BandActivityFeed_IsScheduledType() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            FeedStatus bandFeed = statuses.stream()
                    .filter(f -> FEED_BAND_ACTIVITY.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SCHEDULED, bandFeed.type());
            assertTrue(bandFeed instanceof ScheduledFeedStatus);
            assertEquals(Duration.ofMinutes(1).toSeconds(), ((ScheduledFeedStatus) bandFeed).refreshIntervalSeconds());
        }

        @Test
        @DisplayName("PSKReporter MQTT reports CONNECTED when connected and receiving")
        void testGetFeedStatuses_ConnectedAndReceiving_ReportsConnected() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            SubscriptionFeedStatus mqttFeed = (SubscriptionFeedStatus) statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(ConnectionState.CONNECTED, mqttFeed.connectionState());
            assertEquals(HealthStatus.HEALTHY, mqttFeed.healthStatus());
        }

        @Test
        @DisplayName("PSKReporter MQTT reports STALE when connected but not receiving")
        void testGetFeedStatuses_ConnectedNotReceiving_ReportsStale() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(false);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            SubscriptionFeedStatus mqttFeed = (SubscriptionFeedStatus) statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(ConnectionState.STALE, mqttFeed.connectionState());
            assertEquals(HealthStatus.DEGRADED, mqttFeed.healthStatus());
        }

        @Test
        @DisplayName("PSKReporter MQTT reports DISCONNECTED when not connected")
        void testGetFeedStatuses_NotConnected_ReportsDisconnected() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(false);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            SubscriptionFeedStatus mqttFeed = (SubscriptionFeedStatus) statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(ConnectionState.DISCONNECTED, mqttFeed.connectionState());
            assertEquals(HealthStatus.UNHEALTHY, mqttFeed.healthStatus());
        }

        @Test
        @DisplayName("Band Activity populates execution times from scheduler")
        void testGetFeedStatuses_BandActivityWithExecution_PopulatesExecutionTimes() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(60);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_BAND_ACTIVITY_REFRESH, nextTime, false, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus bandFeed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_BAND_ACTIVITY.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(nextTime, bandFeed.nextRefreshTime());
            assertNotNull(bandFeed.lastRefreshTime());
            assertFalse(bandFeed.isCurrentlyRefreshing());
            assertEquals(HealthStatus.HEALTHY, bandFeed.healthStatus());
        }

        @Test
        @DisplayName("Band Activity reports failures")
        void testGetFeedStatuses_BandActivityWithFailures_ReportsFailures() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(60);
            Instant failureTime = Instant.now().minusSeconds(30);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_BAND_ACTIVITY_REFRESH, nextTime, false, 2, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus bandFeed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_BAND_ACTIVITY.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(2, bandFeed.consecutiveFailures());
            assertEquals(failureTime, bandFeed.lastFailureTime());
            assertEquals(HealthStatus.DEGRADED, bandFeed.healthStatus());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("returns success for Band Activity feed")
        void testTriggerRefresh_BandActivityFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_BAND_ACTIVITY);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_BAND_ACTIVITY, result.get().feedName());
            assertNotNull(result.get().scheduledFor());
            verify(scheduler).reschedule(any(TaskInstanceId.class), any(Instant.class));
        }

        @Test
        @DisplayName("returns notScheduledFeed for PSKReporter MQTT")
        void testTriggerRefresh_PskReporterFeed_ReturnsNotScheduledFeed() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_PSKREPORTER_MQTT);

            // Then
            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals(FEED_PSKREPORTER_MQTT, result.get().feedName());
            assertEquals("Cannot trigger refresh for subscription feeds", result.get().message());
        }

        @Test
        @DisplayName("returns empty for unknown feed")
        void testTriggerRefresh_UnknownFeed_ReturnsEmpty() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh("Unknown Feed");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns failure when scheduler throws exception")
        void testTriggerRefresh_SchedulerThrows_ReturnsFailure() {
            // Given
            doThrow(new RuntimeException("Database connection lost")).when(scheduler).reschedule(any(), any());

            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_BAND_ACTIVITY);

            // Then
            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals(FEED_BAND_ACTIVITY, result.get().feedName());
            assertTrue(result.get().message().contains("Database connection lost"));
        }
    }
}
