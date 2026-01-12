package io.nextskip.admin.api;

import io.nextskip.admin.internal.FeedStatusService;
import io.nextskip.admin.model.FeedStatus;
import io.nextskip.admin.model.FeedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminEndpoint.
 */
@ExtendWith(MockitoExtension.class)
class AdminEndpointTest {

    private static final String FEED_ID = "test-feed";
    private static final String FEED_NAME = "Test Feed";

    @Mock
    private FeedStatusService feedStatusService;

    private AdminEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new AdminEndpoint(feedStatusService);
    }

    @Test
    void testGetFeedStatuses_DelegatesToService() {
        List<FeedStatus> expected = List.of(
                new FeedStatus(FEED_ID, FEED_NAME, FeedType.SCHEDULED, true, Instant.now(), null)
        );
        when(feedStatusService.getAllFeedStatuses()).thenReturn(expected);

        List<FeedStatus> result = endpoint.getFeedStatuses();

        assertThat(result).isEqualTo(expected);
        verify(feedStatusService).getAllFeedStatuses();
    }

    @Test
    void testGetFeedStatuses_EmptyList() {
        when(feedStatusService.getAllFeedStatuses()).thenReturn(List.of());

        List<FeedStatus> result = endpoint.getFeedStatuses();

        assertThat(result).isEmpty();
    }

    @Test
    void testTriggerRefresh_Success_ReturnsTrue() {
        when(feedStatusService.triggerRefresh(FEED_ID)).thenReturn(true);

        boolean result = endpoint.triggerRefresh(FEED_ID);

        assertThat(result).isTrue();
        verify(feedStatusService).triggerRefresh(FEED_ID);
    }

    @Test
    void testTriggerRefresh_NotFound_ReturnsFalse() {
        when(feedStatusService.triggerRefresh(FEED_ID)).thenReturn(false);

        boolean result = endpoint.triggerRefresh(FEED_ID);

        assertThat(result).isFalse();
    }

    @Test
    void testCanRefresh_IsScheduled_ReturnsTrue() {
        when(feedStatusService.isScheduledFeed(FEED_ID)).thenReturn(true);

        boolean result = endpoint.canRefresh(FEED_ID);

        assertThat(result).isTrue();
        verify(feedStatusService).isScheduledFeed(FEED_ID);
    }

    @Test
    void testCanRefresh_NotScheduled_ReturnsFalse() {
        when(feedStatusService.isScheduledFeed(FEED_ID)).thenReturn(false);

        boolean result = endpoint.canRefresh(FEED_ID);

        assertThat(result).isFalse();
    }
}
