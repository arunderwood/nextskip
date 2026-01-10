package io.nextskip.activations.internal.admin;

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
import static io.nextskip.test.TestConstants.FEED_POTA;
import static io.nextskip.test.TestConstants.FEED_SOTA;
import static io.nextskip.test.TestConstants.MODULE_ACTIVATIONS;
import static io.nextskip.test.TestConstants.TASK_POTA_REFRESH;
import static io.nextskip.test.TestConstants.TASK_SOTA_REFRESH;
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
 * Unit tests for {@link ActivationsAdminProvider}.
 *
 * <p>Tests feed status retrieval and trigger refresh functionality
 * for activations module feeds (POTA, SOTA).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivationsAdminProvider")
class ActivationsAdminProviderTest {

    @Mock
    private Scheduler scheduler;

    private ActivationsAdminProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ActivationsAdminProvider(scheduler);
    }

    @Test
    @DisplayName("returns 'activations' as module name")
    void testGetModuleName_ReturnsCorrectName() {
        assertEquals(MODULE_ACTIVATIONS, provider.getModuleName());
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns two feeds for activations module")
        void testGetFeedStatuses_ReturnsTwoFeeds() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();

            // Then
            assertEquals(2, statuses.size());
        }

        @Test
        @DisplayName("returns POTA feed with correct properties")
        void testGetFeedStatuses_PotaFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_POTA.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SCHEDULED, feed.type());
            assertEquals(Duration.ofMinutes(1).toSeconds(), feed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("returns SOTA feed with correct properties")
        void testGetFeedStatuses_SotaFeed_HasCorrectProperties() {
            // Given
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_SOTA.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(FeedType.SCHEDULED, feed.type());
            assertEquals(Duration.ofMinutes(1).toSeconds(), feed.refreshIntervalSeconds());
        }

        @Test
        @DisplayName("populates execution times from scheduler")
        void testGetFeedStatuses_WithExecution_PopulatesExecutionTimes() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(60);
            ScheduledExecution<Object> execution = createMockExecution(TASK_POTA_REFRESH, nextTime, false, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_POTA.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

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
            ScheduledExecution<Object> execution = createMockExecution(TASK_SOTA_REFRESH, nextTime, true, 0, null);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_SOTA.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertTrue(feed.isCurrentlyRefreshing());
        }

        @Test
        @DisplayName("reports consecutive failures")
        void testGetFeedStatuses_WithFailures_ReportsFailureInfo() {
            // Given
            Instant nextTime = Instant.now().plusSeconds(60);
            Instant failureTime = Instant.now().minusSeconds(30);
            ScheduledExecution<Object> execution = createMockExecution(
                    TASK_POTA_REFRESH, nextTime, false, 3, failureTime);
            doReturn(List.of(execution)).when(scheduler).getScheduledExecutions();

            // When
            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                    .filter(f -> FEED_POTA.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            // Then
            assertEquals(3, feed.consecutiveFailures());
            assertEquals(failureTime, feed.lastFailureTime());
            assertEquals(HealthStatus.UNHEALTHY, feed.healthStatus());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("returns success for POTA feed")
        void testTriggerRefresh_PotaFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_POTA);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_POTA, result.get().feedName());
            assertNotNull(result.get().scheduledFor());
            verify(scheduler).reschedule(any(TaskInstanceId.class), any(Instant.class));
        }

        @Test
        @DisplayName("returns success for SOTA feed")
        void testTriggerRefresh_SotaFeed_ReturnsSuccess() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_SOTA);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_SOTA, result.get().feedName());
        }

        @Test
        @DisplayName("returns empty for unknown feed")
        void testTriggerRefresh_UnknownFeed_ReturnsEmpty() {
            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh("Unknown");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns failure when scheduler throws exception")
        void testTriggerRefresh_SchedulerThrows_ReturnsFailure() {
            // Given
            doThrow(new RuntimeException("Connection failed")).when(scheduler).reschedule(any(), any());

            // When
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_POTA);

            // Then
            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertTrue(result.get().message().contains("Connection failed"));
        }
    }
}
