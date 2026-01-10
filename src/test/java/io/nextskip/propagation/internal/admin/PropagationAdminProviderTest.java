package io.nextskip.propagation.internal.admin;

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

import static io.nextskip.test.TestConstants.FEED_HAMQSL_BAND;
import static io.nextskip.test.TestConstants.FEED_HAMQSL_SOLAR;
import static io.nextskip.test.TestConstants.FEED_NOAA_SWPC;
import static io.nextskip.test.TestConstants.MODULE_PROPAGATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PropagationAdminProvider}.
 *
 * <p>Tests provider configuration. Base class behavior (execution times, failures,
 * trigger refresh) is tested in {@code AbstractScheduledAdminProviderTest}.
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

    @Test
    @DisplayName("returns three feeds for propagation module")
    void testGetFeedStatuses_ReturnsThreeFeeds() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();

        assertEquals(3, statuses.size());
    }

    @Test
    @DisplayName("returns NOAA SWPC feed with correct properties")
    void testGetFeedStatuses_NoaaFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                .filter(f -> FEED_NOAA_SWPC.equals(f.name()))
                .findFirst()
                .orElseThrow();

        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofMinutes(5).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }

    @Test
    @DisplayName("returns HamQSL Solar feed with correct properties")
    void testGetFeedStatuses_HamQslSolarFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                .filter(f -> FEED_HAMQSL_SOLAR.equals(f.name()))
                .findFirst()
                .orElseThrow();

        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofMinutes(30).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }

    @Test
    @DisplayName("returns HamQSL Band feed with correct properties")
    void testGetFeedStatuses_HamQslBandFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                .filter(f -> FEED_HAMQSL_BAND.equals(f.name()))
                .findFirst()
                .orElseThrow();

        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofMinutes(15).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }
}
