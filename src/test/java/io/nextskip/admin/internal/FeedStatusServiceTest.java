package io.nextskip.admin.internal;

import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.admin.model.FeedStatus;
import io.nextskip.admin.model.FeedType;
import io.nextskip.common.api.SubscriptionStatusProvider;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeedStatusService.
 *
 * <p>Tests the feed status aggregation, refresh triggering, and edge cases
 * including when the scheduler is disabled.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedStatusServiceTest {

    private static final String TEST_TASK_ID = "test-task";
    private static final String TEST_TASK_DISPLAY = "Test Task";
    private static final String TEST_SUBSCRIPTION_ID = "test-subscription";
    private static final String TEST_SUBSCRIPTION_DISPLAY = "Test Subscription";

    @Mock
    private Scheduler scheduler;

    @Mock
    private ObjectProvider<Scheduler> schedulerProvider;

    @Mock
    private RefreshTaskCoordinator scheduledFeed;

    @Mock
    private SubscriptionStatusProvider subscriptionFeed;

    @Mock
    @SuppressWarnings("unchecked")
    private RecurringTask<Void> recurringTask;

    private FeedStatusService service;

    private void setupScheduledFeedMock() {
        when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
        when(scheduledFeed.getTaskName()).thenReturn(TEST_TASK_ID);
        when(scheduledFeed.getDisplayName()).thenReturn(TEST_TASK_DISPLAY);
        when(scheduledFeed.getRecurringTask()).thenReturn(recurringTask);
    }

    private void setupSubscriptionFeedMock() {
        when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
        when(subscriptionFeed.getSubscriptionId()).thenReturn(TEST_SUBSCRIPTION_ID);
        when(subscriptionFeed.getDisplayName()).thenReturn(TEST_SUBSCRIPTION_DISPLAY);
    }

    @Nested
    class GetAllFeedStatusesTests {

        @Test
        void testGetAllFeedStatuses_EmptyLists_ReturnsEmpty() {
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result).isEmpty();
        }

        @Test
        void testGetAllFeedStatuses_WithScheduledFeed_ReturnsStatus() {
            setupScheduledFeedMock();
            mockSchedulerExecution(Instant.now().minus(Duration.ofMinutes(30)), true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(FeedType.SCHEDULED);
            assertThat(result.get(0).displayName()).isEqualTo("Test Task");
        }

        @Test
        void testGetAllFeedStatuses_WithSubscriptionFeed_ReturnsStatus() {
            setupSubscriptionFeedMock();
            when(subscriptionFeed.isConnected()).thenReturn(true);
            when(subscriptionFeed.getLastMessageTime()).thenReturn(Instant.now());
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    List.of(subscriptionFeed));

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(FeedType.SUBSCRIPTION);
            assertThat(result.get(0).displayName()).isEqualTo("Test Subscription");
        }

        @Test
        void testGetAllFeedStatuses_SortsByDisplayName() {
            RefreshTaskCoordinator aFeed = mock(RefreshTaskCoordinator.class);
            RefreshTaskCoordinator zFeed = mock(RefreshTaskCoordinator.class);
            when(aFeed.getTaskName()).thenReturn("a-task");
            when(aFeed.getDisplayName()).thenReturn("Alpha Task");
            when(aFeed.getRecurringTask()).thenReturn(recurringTask);
            when(zFeed.getTaskName()).thenReturn("z-task");
            when(zFeed.getDisplayName()).thenReturn("Zeta Task");
            when(zFeed.getRecurringTask()).thenReturn(recurringTask);
            mockSchedulerExecution(Instant.now(), true);

            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(zFeed, aFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).displayName()).isEqualTo("Alpha Task");
            assertThat(result.get(1).displayName()).isEqualTo("Zeta Task");
        }
    }

    @Nested
    class GetFeedStatusTests {

        @Test
        void testGetFeedStatus_ScheduledFeedFound_ReturnsStatus() {
            setupScheduledFeedMock();
            mockSchedulerExecution(Instant.now(), true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            Optional<FeedStatus> result = service.getFeedStatus(TEST_TASK_ID);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(TEST_TASK_ID);
        }

        @Test
        void testGetFeedStatus_SubscriptionFeedFound_ReturnsStatus() {
            setupSubscriptionFeedMock();
            when(subscriptionFeed.isConnected()).thenReturn(true);
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    List.of(subscriptionFeed));

            Optional<FeedStatus> result = service.getFeedStatus(TEST_SUBSCRIPTION_ID);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(TEST_SUBSCRIPTION_ID);
        }

        @Test
        void testGetFeedStatus_NotFound_ReturnsEmpty() {
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    Collections.emptyList());

            Optional<FeedStatus> result = service.getFeedStatus("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class TriggerRefreshTests {

        @Test
        void testTriggerRefresh_FeedFound_ReschedulesTask() {
            setupScheduledFeedMock();
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            boolean result = service.triggerRefresh(TEST_TASK_ID);

            assertThat(result).isTrue();
            verify(scheduler).reschedule(any(), any(Instant.class));
        }

        @Test
        void testTriggerRefresh_FeedNotFound_ReturnsFalse() {
            when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    Collections.emptyList());

            boolean result = service.triggerRefresh("unknown");

            assertThat(result).isFalse();
            verify(scheduler, never()).reschedule(any(), any());
        }

        @Test
        void testTriggerRefresh_SchedulerDisabled_ReturnsFalse() {
            when(schedulerProvider.getIfAvailable()).thenReturn(null);
            when(scheduledFeed.getTaskName()).thenReturn(TEST_TASK_ID);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            boolean result = service.triggerRefresh(TEST_TASK_ID);

            assertThat(result).isFalse();
        }

        @Test
        void testTriggerRefresh_SchedulerThrows_ReturnsFalse() {
            setupScheduledFeedMock();
            when(scheduler.reschedule(any(), any(Instant.class)))
                    .thenThrow(new RuntimeException("Scheduler error"));
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            boolean result = service.triggerRefresh(TEST_TASK_ID);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class IsScheduledFeedTests {

        @Test
        void testIsScheduledFeed_Found_ReturnsTrue() {
            when(scheduledFeed.getTaskName()).thenReturn(TEST_TASK_ID);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            assertThat(service.isScheduledFeed(TEST_TASK_ID)).isTrue();
        }

        @Test
        void testIsScheduledFeed_NotFound_ReturnsFalse() {
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    Collections.emptyList());

            assertThat(service.isScheduledFeed("unknown")).isFalse();
        }

        @Test
        void testIsScheduledFeed_WrongId_ReturnsFalse() {
            when(scheduledFeed.getTaskName()).thenReturn(TEST_TASK_ID);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            // Specifically tests that task names are compared correctly
            assertThat(service.isScheduledFeed("other-task")).isFalse();
        }

        @Test
        void testIsScheduledFeed_MultipleFeeds_MatchesCorrectOne() {
            RefreshTaskCoordinator otherFeed = mock(RefreshTaskCoordinator.class);
            when(scheduledFeed.getTaskName()).thenReturn(TEST_TASK_ID);
            when(otherFeed.getTaskName()).thenReturn("other-task");

            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed, otherFeed),
                    Collections.emptyList());

            assertThat(service.isScheduledFeed(TEST_TASK_ID)).isTrue();
            assertThat(service.isScheduledFeed("other-task")).isTrue();
            assertThat(service.isScheduledFeed("nonexistent")).isFalse();
        }
    }

    @Nested
    class ScheduledFeedStatusTests {

        @Test
        void testScheduledFeed_HealthyWithRecentExecution() {
            setupScheduledFeedMock();
            Instant recentExecution = Instant.now().minus(Duration.ofMinutes(30));
            mockSchedulerExecution(recentExecution, true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isTrue();
            assertThat(result.get(0).statusMessage()).isNull(); // Healthy feeds have no message
        }

        @Test
        void testScheduledFeed_UnhealthyWhenStale_HoursFormat() {
            setupScheduledFeedMock();
            Instant staleExecution = Instant.now().minus(Duration.ofHours(3));
            mockSchedulerExecution(staleExecution, true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).contains("Stale");
            assertThat(result.get(0).statusMessage()).contains("3h");
        }

        @Test
        void testScheduledFeed_StaleMessage_MinutesFormat() {
            setupScheduledFeedMock();
            // Just over 2 hours but less than 3 to show minutes format would be used
            Instant staleExecution = Instant.now().minus(Duration.ofMinutes(121));
            mockSchedulerExecution(staleExecution, true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).contains("2h");
        }

        @Test
        void testScheduledFeed_JustBeforeStaleBoundary_IsStillHealthy() {
            setupScheduledFeedMock();
            // Just before the 2-hour boundary should still be healthy
            Instant beforeBoundaryExecution = Instant.now().minus(Duration.ofMinutes(119));
            mockSchedulerExecution(beforeBoundaryExecution, true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            // At 119 minutes (just under 2 hours), should be healthy
            assertThat(result.get(0).healthy()).isTrue();
        }

        @Test
        void testScheduledFeed_JustPastStaleBoundary_IsUnhealthy() {
            setupScheduledFeedMock();
            // Just past the 2-hour boundary should be unhealthy
            Instant pastBoundaryExecution = Instant.now()
                    .minus(Duration.ofHours(2))
                    .minus(Duration.ofSeconds(1));
            mockSchedulerExecution(pastBoundaryExecution, true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).contains("Stale");
        }

        @Test
        void testScheduledFeed_UnhealthyWhenNeverExecuted() {
            setupScheduledFeedMock();
            mockSchedulerExecution(null, true);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).isEqualTo("Never executed");
        }

        @Test
        void testScheduledFeed_UnhealthyWhenNotScheduled() {
            setupScheduledFeedMock();
            when(scheduler.getScheduledExecution(any(TaskInstanceId.class)))
                    .thenReturn(Optional.empty());
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).isEqualTo("Not scheduled");
        }

        @Test
        void testScheduledFeed_SchedulerDisabled_ShowsUnavailable() {
            when(schedulerProvider.getIfAvailable()).thenReturn(null);
            when(scheduledFeed.getTaskName()).thenReturn(TEST_TASK_ID);
            when(scheduledFeed.getDisplayName()).thenReturn(TEST_TASK_DISPLAY);
            service = new FeedStatusService(
                    schedulerProvider,
                    List.of(scheduledFeed),
                    Collections.emptyList());

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).isEqualTo("Scheduler disabled");
        }
    }

    @Nested
    class SubscriptionFeedStatusTests {

        @Test
        void testSubscriptionFeed_Connected_IsHealthy() {
            setupSubscriptionFeedMock();
            when(subscriptionFeed.isConnected()).thenReturn(true);
            when(subscriptionFeed.getLastMessageTime()).thenReturn(Instant.now());
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    List.of(subscriptionFeed));

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isTrue();
            assertThat(result.get(0).statusMessage()).isEqualTo("Connected");
        }

        @Test
        void testSubscriptionFeed_Disconnected_IsUnhealthy() {
            setupSubscriptionFeedMock();
            when(subscriptionFeed.isConnected()).thenReturn(false);
            when(subscriptionFeed.getLastMessageTime()).thenReturn(Instant.now().minus(Duration.ofMinutes(10)));
            service = new FeedStatusService(
                    schedulerProvider,
                    Collections.emptyList(),
                    List.of(subscriptionFeed));

            List<FeedStatus> result = service.getAllFeedStatuses();

            assertThat(result.get(0).healthy()).isFalse();
            assertThat(result.get(0).statusMessage()).isEqualTo("Disconnected");
        }
    }

    @SuppressWarnings("unchecked")
    private void mockSchedulerExecution(Instant lastSuccess, boolean scheduled) {
        if (scheduled) {
            ScheduledExecution<Object> execution = mock(ScheduledExecution.class);
            when(execution.getLastSuccess()).thenReturn(lastSuccess);
            when(scheduler.getScheduledExecution(any(TaskInstanceId.class)))
                    .thenReturn(Optional.of(execution));
        } else {
            when(scheduler.getScheduledExecution(any(TaskInstanceId.class)))
                    .thenReturn(Optional.empty());
        }
    }
}
