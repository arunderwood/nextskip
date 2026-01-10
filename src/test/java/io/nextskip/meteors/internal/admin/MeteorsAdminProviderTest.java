package io.nextskip.meteors.internal.admin;

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

import static io.nextskip.test.TestConstants.FEED_METEOR_SHOWERS;
import static io.nextskip.test.TestConstants.MODULE_METEORS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MeteorsAdminProvider}.
 *
 * <p>Tests provider configuration. Base class behavior (execution times, failures,
 * trigger refresh) is tested in {@code AbstractScheduledAdminProviderTest}.
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

    @Test
    @DisplayName("returns one feed for meteors module")
    void testGetFeedStatuses_ReturnsOneFeed() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();

        assertEquals(1, statuses.size());
    }

    @Test
    @DisplayName("returns Meteor Showers feed with correct properties")
    void testGetFeedStatuses_MeteorShowersFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.get(0);

        assertEquals(FEED_METEOR_SHOWERS, feed.name());
        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofHours(1).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }
}
