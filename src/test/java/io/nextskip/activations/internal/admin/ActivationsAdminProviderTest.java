package io.nextskip.activations.internal.admin;

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

import static io.nextskip.test.TestConstants.FEED_POTA;
import static io.nextskip.test.TestConstants.FEED_SOTA;
import static io.nextskip.test.TestConstants.MODULE_ACTIVATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ActivationsAdminProvider}.
 *
 * <p>Tests provider configuration. Base class behavior (execution times, failures,
 * trigger refresh) is tested in {@code AbstractScheduledAdminProviderTest}.
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

    @Test
    @DisplayName("returns two feeds for activations module")
    void testGetFeedStatuses_ReturnsTwoFeeds() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();

        assertEquals(2, statuses.size());
    }

    @Test
    @DisplayName("returns POTA feed with correct properties")
    void testGetFeedStatuses_PotaFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                .filter(f -> FEED_POTA.equals(f.name()))
                .findFirst()
                .orElseThrow();

        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofMinutes(1).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }

    @Test
    @DisplayName("returns SOTA feed with correct properties")
    void testGetFeedStatuses_SotaFeed_HasCorrectProperties() {
        when(scheduler.getScheduledExecutions()).thenReturn(List.of());

        List<FeedStatus> statuses = provider.getFeedStatuses();
        ScheduledFeedStatus feed = (ScheduledFeedStatus) statuses.stream()
                .filter(f -> FEED_SOTA.equals(f.name()))
                .findFirst()
                .orElseThrow();

        assertEquals(FeedType.SCHEDULED, feed.type());
        assertEquals(Duration.ofMinutes(1).toSeconds(), feed.refreshIntervalSeconds());
        assertTrue(feed instanceof ScheduledFeedStatus);
    }
}
