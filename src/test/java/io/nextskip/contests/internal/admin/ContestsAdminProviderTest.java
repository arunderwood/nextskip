package io.nextskip.contests.internal.admin;

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
import static io.nextskip.test.TestConstants.FEED_CONTEST_CALENDAR;
import static io.nextskip.test.TestConstants.MODULE_CONTESTS;
import static io.nextskip.test.TestConstants.TASK_CONTEST_REFRESH;
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
 * Unit tests for {@link ContestsAdminProvider}.
 *
 * <p>Tests feed status retrieval and trigger refresh functionality
 * for contests module feeds (Contest Calendar).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContestsAdminProvider")
class ContestsAdminProviderTest {

    @Mock
    private Scheduler scheduler;

    private ContestsAdminProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ContestsAdminProvider(scheduler);
    }

    @Test
    @DisplayName("returns 'contests' as module name")
    void testGetModuleName_ReturnsCorrectName() {
        assertEquals(MODULE_CONTESTS, provider.getModuleName());
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns one feed for contests module")
        void testGetFeedStatuses_ReturnsOneFeed() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();

            // Then
            assertEquals(1, statuses.size());
        }

        @Test
        @DisplayName("returns Contest Calendar feed with correct properties")
        void testGetFeedStatuses_ContestCalendarFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(FEED_CONTEST_CALENDAR, feed.name());
            assertEquals(FeedType.SCHEDULED, feed.type());
            assertEquals(Duration.ofHours(6).toSeconds(), feed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("populates execution times from scheduler")
        void testGetFeedStatuses_WithExecution_PopulatesExecutionTimes() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(21600); // 6 hours
            ScheduledExecution<Object> execution = createMockExecution(TASK_CONTEST_REFRESH, nextTime, false, 0, null);
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
            ScheduledExecution<Object> execution = createMockExecution(TASK_CONTEST_REFRESH, nextTime, true, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertTrue(feed.isCurrentlyRefreshing());
        }

        @Test
        @DisplayName("reports degraded status with one failure")
        void testGetFeedStatuses_OneFailure_ReportsDegraded() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(21600);
            Instant failureTime = Instant.now().minusSeconds(300);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_CONTEST_REFRESH, nextTime, false, 1, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(1, feed.consecutiveFailures());
            assertEquals(failureTime, feed.lastFailureTime());
            assertEquals(HealthStatus.DEGRADED, feed.healthStatus());
        }

        @Test
        @DisplayName("reports unhealthy status with three or more failures")
        void testGetFeedStatuses_ThreeFailures_ReportsUnhealthy() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(21600);
            Instant failureTime = Instant.now().minusSeconds(300);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_CONTEST_REFRESH, nextTime, false, 3, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

            // Then
            assertEquals(3, feed.consecutiveFailures());
            assertEquals(HealthStatus.UNHEALTHY, feed.healthStatus());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("returns success for Contest Calendar feed")
        void testTriggerRefresh_ContestCalendarFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_CONTEST_CALENDAR);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_CONTEST_CALENDAR, result.get().feedName());
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
            doThrow(new RuntimeException("Database error")).when(scheduler).reschedule(any(), any());

            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_CONTEST_CALENDAR);

            // Then
            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals(FEED_CONTEST_CALENDAR, result.get().feedName());
            assertTrue(result.get().message().contains("Database error"));
        }
    }
}
