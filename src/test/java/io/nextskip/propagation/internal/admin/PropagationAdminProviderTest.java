package io.nextskip.propagation.internal.admin;

import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.FeedType;
import io.nextskip.common.admin.HealthStatus;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
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
import static io.nextskip.test.TestConstants.FEED_HAMQSL_BAND;
import static io.nextskip.test.TestConstants.FEED_HAMQSL_SOLAR;
import static io.nextskip.test.TestConstants.FEED_NOAA_SWPC;
import static io.nextskip.test.TestConstants.MODULE_PROPAGATION;
import static io.nextskip.test.TestConstants.TASK_NOAA_REFRESH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PropagationAdminProvider}.
 *
 * <p>Tests feed status retrieval and trigger refresh functionality
 * for propagation module feeds (NOAA, HamQSL Solar, HamQSL Band).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PropagationAdminProvider")
class PropagationAdminProviderTest {

    @Mock
    private Scheduler scheduler;

    private PropagationAdminProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PropagationAdminProvider(scheduler);
    }

    @Test
    @DisplayName("returns 'propagation' as module name")
    void testGetModuleName_ReturnsCorrectName() {
        assertEquals(MODULE_PROPAGATION, provider.getModuleName());
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns three feeds for propagation module")
        void testGetFeedStatuses_ReturnsThreeFeeds() {
            // Given: No scheduled executions
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();

            // Then
            assertEquals(3, statuses.size());
        }

        @Test
        @DisplayName("returns NOAA SWPC feed with correct properties")
        void testGetFeedStatuses_NoaaFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus noaaFeed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SCHEDULED, noaaFeed.type());
            assertEquals(Duration.ofMinutes(5).toSeconds(), noaaFeed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("returns HamQSL Solar feed with correct properties")
        void testGetFeedStatuses_HamQslSolarFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_HAMQSL_SOLAR.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SCHEDULED, feed.type());
            assertEquals(Duration.ofMinutes(30).toSeconds(), feed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("returns HamQSL Band feed with correct properties")
        void testGetFeedStatuses_HamQslBandFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_HAMQSL_BAND.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SCHEDULED, feed.type());
            assertEquals(Duration.ofMinutes(15).toSeconds(), feed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("populates execution times from scheduler")
        void testGetFeedStatuses_WithExecution_PopulatesExecutionTimes() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(300);
            ScheduledExecution<Object> execution = createMockExecution(TASK_NOAA_REFRESH, nextTime, false, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(nextTime, feed.nextRefreshTime());
            assertNotNull(feed.lastRefreshTime());
            assertFalse(feed.isCurrentlyRefreshing());
        }

        @Test
        @DisplayName("sets isCurrentlyRefreshing when task is picked")
        void testGetFeedStatuses_TaskPicked_IsCurrentlyRefreshingTrue() {
            // Given
            Instant nextTime = Instant.now();
            ScheduledExecution<Object> execution = createMockExecution(TASK_NOAA_REFRESH, nextTime, true, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertTrue(feed.isCurrentlyRefreshing());
            assertNull(feed.lastRefreshTime()); // Null when currently refreshing
        }

        @Test
        @DisplayName("reports consecutive failures and last failure time")
        void testGetFeedStatuses_WithFailures_ReportsFailureInfo() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(300);
            Instant failureTime = Instant.now().minusSeconds(60);
            ScheduledExecution<Object> execution = createMockExecution(TASK_NOAA_REFRESH, nextTime, false, 2, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(2, feed.consecutiveFailures());
            assertEquals(failureTime, feed.lastFailureTime());
            assertEquals(HealthStatus.DEGRADED, feed.healthStatus());
        }

        @Test
        @DisplayName("reports healthy status when no failures")
        void testGetFeedStatuses_NoFailures_ReportsHealthy() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(300);
            ScheduledExecution<Object> execution = createMockExecution(TASK_NOAA_REFRESH, nextTime, false, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(HealthStatus.HEALTHY, feed.healthStatus());
        }

        @Test
        @DisplayName("reports unhealthy status when many failures")
        void testGetFeedStatuses_ManyFailures_ReportsUnhealthy() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(300);
            Instant failureTime = Instant.now().minusSeconds(60);
            ScheduledExecution<Object> execution = createMockExecution(TASK_NOAA_REFRESH, nextTime, false, 5, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(HealthStatus.UNHEALTHY, feed.healthStatus());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("returns success for NOAA SWPC feed")
        void testTriggerRefresh_NoaaFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_NOAA_SWPC);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_NOAA_SWPC, result.get().feedName());
            assertNotNull(result.get().scheduledFor());
            verify(scheduler).reschedule(any(TaskInstanceId.class), any(Instant.class));
        }

        @Test
        @DisplayName("returns success for HamQSL Solar feed")
        void testTriggerRefresh_HamQslSolarFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_HAMQSL_SOLAR);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_HAMQSL_SOLAR, result.get().feedName());
        }

        @Test
        @DisplayName("returns success for HamQSL Band feed")
        void testTriggerRefresh_HamQslBandFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_HAMQSL_BAND);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_HAMQSL_BAND, result.get().feedName());
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
            doThrow(new RuntimeException("Scheduler error")).when(scheduler).reschedule(any(), any());

            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_NOAA_SWPC);

            // Then
            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals(FEED_NOAA_SWPC, result.get().feedName());
            assertTrue(result.get().message().contains("Scheduler error"));
        }
    }
}
