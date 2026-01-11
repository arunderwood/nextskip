package io.nextskip.contests.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.FeedType;
import io.nextskip.common.admin.ScheduledFeedStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static io.nextskip.test.TestConstants.FEED_CONTEST_CALENDAR;
import static io.nextskip.test.TestConstants.MODULE_CONTESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContestsAdminProvider}.
 *
 * <p>Tests provider configuration. Base class behavior (execution times, failures,
 * trigger refresh) is tested in {@code AbstractScheduledAdminProviderTest}.
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

    @Test
    @DisplayName("returns one feed for contests module")
    void testGetFeedStatuses_ReturnsOneFeed() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();

        assertEquals(1, statuses.size());
    }

    @Test
    @DisplayName("returns Contest Calendar feed with correct properties")
    void testGetFeedStatuses_ContestCalendarFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

        assertEquals(FEED_CONTEST_CALENDAR, feed.name());
        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofHours(6).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }
}
