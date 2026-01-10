package io.nextskip.common.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.nextskip.common.admin.AdminProviderTestFixtures.createHealthyExecution;
import static io.nextskip.common.admin.AdminProviderTestFixtures.createRunningExecution;
import static io.nextskip.common.admin.AdminProviderTestFixtures.createUnhealthyExecution;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractScheduledAdminProvider}.
 *
 * <p>Uses a concrete test implementation to verify base class behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractScheduledAdminProvider")
class AbstractScheduledAdminProviderTest {

    private static final String MODULE_NAME = "test-module";
    private static final String TASK_NAME = "test-task";
    private static final String FEED_NAME = "Test Feed";
    private static final long INTERVAL_SECONDS = 300L;

    @Mock
    private Scheduler scheduler;

    private TestScheduledAdminProvider provider;

    @BeforeEach
    void setUp() {
        List<ScheduledFeedDefinition> feeds = List.of(
                new ScheduledFeedDefinition(FEED_NAME, TASK_NAME, INTERVAL_SECONDS)
        );
        provider = new TestScheduledAdminProvider(scheduler, MODULE_NAME, feeds);
    }

    @Nested
    @DisplayName("getModuleName")
    class GetModuleNameTests {

        @Test
        @DisplayName("returns configured module name")
        void testGetModuleName_ReturnsConfiguredName() {
            assertEquals(MODULE_NAME, provider.getModuleName());
        }
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns status for each feed definition")
        void testGetFeedStatuses_ReturnsStatusForEachFeed() {
            Instant nextExecution = Instant.now().plusSeconds(INTERVAL_SECONDS);
            ScheduledExecution<Object> execution = createHealthyExecution(TASK_NAME, nextExecution);
            when(scheduler.getScheduledExecutions()).thenReturn(List.of(execution));

            List<FeedStatus> statuses = provider.getFeedStatuses();

            assertEquals(1, statuses.size());
            assertTrue(statuses.get(0) instanceof ScheduledFeedStatus);
            assertEquals(FEED_NAME, ((ScheduledFeedStatus) statuses.get(0)).name());
        }

        @Test
        @DisplayName("returns multiple statuses for multiple feeds")
        void testGetFeedStatuses_MultipleFeedsReturnsMultipleStatuses() {
            List<ScheduledFeedDefinition> multipleFeeds = List.of(
                    new ScheduledFeedDefinition("Feed One", "task-one", 60L),
                    new ScheduledFeedDefinition("Feed Two", "task-two", 120L)
            );
            provider = new TestScheduledAdminProvider(scheduler, MODULE_NAME, multipleFeeds);

            Instant now = Instant.now();
            ScheduledExecution<Object> exec1 = createHealthyExecution("task-one", now.plusSeconds(60));
            ScheduledExecution<Object> exec2 = createHealthyExecution("task-two", now.plusSeconds(120));
            when(scheduler.getScheduledExecutions()).thenReturn(List.of(exec1, exec2));

            List<FeedStatus> statuses = provider.getFeedStatuses();

            assertEquals(2, statuses.size());
        }

        @Test
        @DisplayName("handles empty execution list")
        void testGetFeedStatuses_NoExecutionsReturnsStatusWithNulls() {
            when(scheduler.getScheduledExecutions()).thenReturn(Collections.emptyList());

            List<FeedStatus> statuses = provider.getFeedStatuses();

            assertEquals(1, statuses.size());
            ScheduledFeedStatus status = (ScheduledFeedStatus) statuses.get(0);
            assertNull(status.lastRefreshTime());
            assertNull(status.nextRefreshTime());
        }
    }

    @Nested
    @DisplayName("buildScheduledFeedStatus")
    class BuildScheduledFeedStatusTests {

        @Test
        @DisplayName("calculates last refresh time from next execution and interval")
        void testBuildScheduledFeedStatus_CalculatesLastRefreshTime() {
            Instant nextExecution = Instant.now().plusSeconds(INTERVAL_SECONDS);
            ScheduledExecution<Object> execution = createHealthyExecution(TASK_NAME, nextExecution);
            when(scheduler.getScheduledExecutions()).thenReturn(List.of(execution));

            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus status = (ScheduledFeedStatus) statuses.get(0);

            assertNotNull(status.lastRefreshTime());
            assertEquals(nextExecution.minusSeconds(INTERVAL_SECONDS), status.lastRefreshTime());
        }

        @Test
        @DisplayName("detects currently refreshing state")
        void testBuildScheduledFeedStatus_DetectsRefreshingState() {
            ScheduledExecution<Object> execution = createRunningExecution(TASK_NAME);
            when(scheduler.getScheduledExecutions()).thenReturn(List.of(execution));

            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus status = (ScheduledFeedStatus) statuses.get(0);

            assertTrue(status.isCurrentlyRefreshing());
            // Last refresh time should be null when running
            assertNull(status.lastRefreshTime());
        }

        @Test
        @DisplayName("captures failure information")
        void testBuildScheduledFeedStatus_CapturesFailureInfo() {
            Instant lastFailure = Instant.now().minusSeconds(60);
            Instant nextExecution = Instant.now().plusSeconds(INTERVAL_SECONDS);
            ScheduledExecution<Object> execution = createUnhealthyExecution(
                    TASK_NAME, nextExecution, 3, lastFailure);
            when(scheduler.getScheduledExecutions()).thenReturn(List.of(execution));

            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus status = (ScheduledFeedStatus) statuses.get(0);

            assertEquals(3, status.consecutiveFailures());
            assertEquals(lastFailure, status.lastFailureTime());
        }

        @Test
        @DisplayName("does not set last failure time when no failures")
        void testBuildScheduledFeedStatus_NoFailuresNoLastFailureTime() {
            Instant nextExecution = Instant.now().plusSeconds(INTERVAL_SECONDS);
            ScheduledExecution<Object> execution = createHealthyExecution(TASK_NAME, nextExecution);
            when(scheduler.getScheduledExecutions()).thenReturn(List.of(execution));

            List<FeedStatus> statuses = provider.getFeedStatuses();
            ScheduledFeedStatus status = (ScheduledFeedStatus) statuses.get(0);

            assertEquals(0, status.consecutiveFailures());
            assertNull(status.lastFailureTime());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("reschedules task for known feed")
        void testTriggerRefresh_KnownFeed_ReschedulesTask() {
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_NAME);

            assertTrue(result.isPresent());
            assertTrue(result.get().success());
            assertEquals(FEED_NAME, result.get().feedName());
            assertNotNull(result.get().scheduledFor());
            verify(scheduler).reschedule(any(), any());
        }

        @Test
        @DisplayName("returns empty for unknown feed")
        void testTriggerRefresh_UnknownFeed_ReturnsEmpty() {
            Optional<TriggerRefreshResult> result = provider.triggerRefresh("Unknown Feed");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns failure result when scheduler throws")
        void testTriggerRefresh_SchedulerException_ReturnsFailure() {
            doThrow(new RuntimeException("Scheduler error"))
                    .when(scheduler).reschedule(any(), any());

            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_NAME);

            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertTrue(result.get().message().contains("Scheduler error"));
        }
    }

    @Nested
    @DisplayName("handleNonScheduledFeed hook")
    class HandleNonScheduledFeedTests {

        @Test
        @DisplayName("default implementation returns empty")
        void testHandleNonScheduledFeed_DefaultReturnsEmpty() {
            Optional<TriggerRefreshResult> result = provider.handleNonScheduledFeed("Any Feed");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("overridden hook intercepts non-scheduled feeds")
        void testHandleNonScheduledFeed_OverrideInterceptsFeed() {
            // Create a provider that overrides the hook
            TestMixedAdminProvider mixedProvider = new TestMixedAdminProvider(scheduler);

            Optional<TriggerRefreshResult> result = mixedProvider.triggerRefresh("Subscription Feed");

            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals("Cannot trigger refresh for subscription feeds", result.get().message());
        }
    }

    @Nested
    @DisplayName("getFeedDefinitions")
    class GetFeedDefinitionsTests {

        @Test
        @DisplayName("returns immutable copy of definitions")
        void testGetFeedDefinitions_ReturnsImmutableCopy() {
            List<ScheduledFeedDefinition> definitions = provider.getFeedDefinitions();

            assertEquals(1, definitions.size());
            assertEquals(FEED_NAME, definitions.get(0).displayName());
        }
    }

    /**
     * Concrete test implementation of AbstractScheduledAdminProvider.
     */
    @SuppressWarnings("PMD.TestClassWithoutTestCases") // Test fixture, not a test class
    private static class TestScheduledAdminProvider extends AbstractScheduledAdminProvider {

        TestScheduledAdminProvider(
                Scheduler scheduler,
                String moduleName,
                List<ScheduledFeedDefinition> feedDefinitions) {
            super(scheduler, moduleName, feedDefinitions);
        }

        // Expose protected method for testing
        @Override
        public Optional<TriggerRefreshResult> handleNonScheduledFeed(String feedName) {
            return super.handleNonScheduledFeed(feedName);
        }
    }

    /**
     * Test implementation that demonstrates mixed feed types.
     */
    @SuppressWarnings("PMD.TestClassWithoutTestCases") // Test fixture, not a test class
    private static class TestMixedAdminProvider extends AbstractScheduledAdminProvider {

        private static final String SUBSCRIPTION_FEED = "Subscription Feed";

        TestMixedAdminProvider(Scheduler scheduler) {
            super(scheduler, "mixed", List.of(
                    new ScheduledFeedDefinition("Scheduled Feed", "scheduled-task", 60L)
            ));
        }

        @Override
        protected Optional<TriggerRefreshResult> handleNonScheduledFeed(String feedName) {
            if (SUBSCRIPTION_FEED.equals(feedName)) {
                return Optional.of(TriggerRefreshResult.notScheduledFeed(feedName));
            }
            return Optional.empty();
        }
    }
}
