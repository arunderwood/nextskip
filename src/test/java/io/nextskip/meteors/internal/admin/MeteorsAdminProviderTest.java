package io.nextskip.meteors.internal.admin;

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
import static io.nextskip.test.TestConstants.FEED_METEOR_SHOWERS;
import static io.nextskip.test.TestConstants.MODULE_METEORS;
import static io.nextskip.test.TestConstants.TASK_METEOR_REFRESH;
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
 * Unit tests for {@link MeteorsAdminProvider}.
 *
 * <p>Tests feed status retrieval and trigger refresh functionality
 * for meteors module feeds (Meteor Showers).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeteorsAdminProvider")
class MeteorsAdminProviderTest {

    @Mock
    private Scheduler scheduler;

    private MeteorsAdminProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MeteorsAdminProvider(scheduler);
    }

    @Test
    @DisplayName("returns 'meteors' as module name")
    void testGetModuleName_ReturnsCorrectName() {
        assertEquals(MODULE_METEORS, provider.getModuleName());
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns one feed for meteors module")
        void testGetFeedStatuses_ReturnsOneFeed() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();

            // Then
            assertEquals(1, statuses.size());
        }

        @Test
        @DisplayName("returns Meteor Showers feed with correct properties")
        void testGetFeedStatuses_MeteorShowersFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(FEED_METEOR_SHOWERS, feed.name());
            assertEquals(FeedType.SCHEDULED, feed.type());
            assertEquals(Duration.ofHours(1).toSeconds(), feed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("populates execution times from scheduler")
        void testGetFeedStatuses_WithExecution_PopulatesExecutionTimes() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(3600); // 1 hour
            ScheduledExecution<Object> execution = createMockExecution(TASK_METEOR_REFRESH, nextTime, false, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(nextTime, feed.nextRefreshTime());
            assertNotNull(feed.lastRefreshTime());
            assertFalse(feed.isCurrentlyRefreshing());
            assertEquals(HealthStatus.HEALTHY, feed.healthStatus());
        }

        @Test
        @DisplayName("sets isCurrentlyRefreshing when task is picked")
        void testGetFeedStatuses_TaskPicked_IsCurrentlyRefreshingTrue() {
            // Given
            Instant nextTime = Instant.now();
            ScheduledExecution<Object> execution = createMockExecution(TASK_METEOR_REFRESH, nextTime, true, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertTrue(feed.isCurrentlyRefreshing());
        }

        @Test
        @DisplayName("reports consecutive failures and last failure time")
        void testGetFeedStatuses_WithFailures_ReportsFailureInfo() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(3600);
            Instant failureTime = Instant.now().minusSeconds(600);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_METEOR_REFRESH, nextTime, false, 2, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(2, feed.consecutiveFailures());
            assertEquals(failureTime, feed.lastFailureTime());
            assertEquals(HealthStatus.DEGRADED, feed.healthStatus());
        }

        @Test
        @DisplayName("reports unhealthy status with many failures")
        void testGetFeedStatuses_ManyFailures_ReportsUnhealthy() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(3600);
            Instant failureTime = Instant.now().minusSeconds(600);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_METEOR_REFRESH, nextTime, false, 5, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(5, feed.consecutiveFailures());
            assertEquals(HealthStatus.UNHEALTHY, feed.healthStatus());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("returns success for Meteor Showers feed")
        void testTriggerRefresh_MeteorShowersFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_METEOR_SHOWERS);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_METEOR_SHOWERS, result.get().feedName());
            assertNotNull(result.get().scheduledFor());
            verify(scheduler).reschedule(any(TaskInstanceId.class), any(Instant.class));
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
            doThrow(new RuntimeException("Task not found")).when(scheduler).reschedule(any(), any());

            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_METEOR_SHOWERS);

            // Then
            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals(FEED_METEOR_SHOWERS, result.get().feedName());
            assertTrue(result.get().message().contains("Task not found"));
        }
    }
}
