package io.nextskip.spots.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.common.admin.ConnectionState;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.FeedType;
import io.nextskip.common.admin.HealthStatus;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.SubscriptionFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
import io.nextskip.spots.internal.client.SpotSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.nextskip.test.TestConstants.FEED_BAND_ACTIVITY;
import static io.nextskip.test.TestConstants.FEED_PSKREPORTER_MQTT;
import static io.nextskip.test.TestConstants.MODULE_SPOTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpotsAdminProvider}.
 *
 * <p>Tests provider configuration and subscription feed-specific behavior.
 * Base class behavior (execution times, failures, trigger refresh) is tested
 * in {@code AbstractScheduledAdminProviderTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotsAdminProvider")
class SpotsAdminProviderTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private SpotSource spotSource;

    private SpotsAdminProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SpotsAdminProvider(scheduler, spotSource);
    }

    @Test
    @DisplayName("returns 'spots' as module name")
    void testGetModuleName_ReturnsCorrectName() {
        assertEquals(MODULE_SPOTS, provider.getModuleName());
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("returns two feeds for spots module")
        void testGetFeedStatuses_ReturnsTwoFeeds() {
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            List<FeedStatus> statuses = provider.getFeedStatuses();

            assertEquals(2, statuses.size());
        }

        @Test
        @DisplayName("returns PSKReporter MQTT as subscription feed")
        void testGetFeedStatuses_PskReporterFeed_IsSubscriptionType() {
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            List<FeedStatus> statuses = provider.getFeedStatuses();
            FeedStatus mqttFeed = statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(FeedType.SUBSCRIPTION, mqttFeed.type());
            assertTrue(mqttFeed instanceof SubscriptionFeedStatus);
        }

        @Test
        @DisplayName("returns Band Activity as scheduled feed")
        void testGetFeedStatuses_BandActivityFeed_IsScheduledType() {
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            List<FeedStatus> statuses = provider.getFeedStatuses();
            FeedStatus bandFeed = statuses.stream()
                    .filter(f -> FEED_BAND_ACTIVITY.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(FeedType.SCHEDULED, bandFeed.type());
            assertTrue(bandFeed instanceof ScheduledFeedStatus);
            assertEquals(Duration.ofMinutes(1).toSeconds(),
                    ((ScheduledFeedStatus) bandFeed).refreshIntervalSeconds());
        }

        @Test
        @DisplayName("PSKReporter MQTT reports CONNECTED when connected and receiving")
        void testGetFeedStatuses_ConnectedAndReceiving_ReportsConnected() {
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(true);

            List<FeedStatus> statuses = provider.getFeedStatuses();
            SubscriptionFeedStatus mqttFeed = (SubscriptionFeedStatus) statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(ConnectionState.CONNECTED, mqttFeed.connectionState());
            assertEquals(HealthStatus.HEALTHY, mqttFeed.healthStatus());
        }

        @Test
        @DisplayName("PSKReporter MQTT reports STALE when connected but not receiving")
        void testGetFeedStatuses_ConnectedNotReceiving_ReportsStale() {
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(true);
            when(spotSource.isReceivingMessages()).thenReturn(false);

            List<FeedStatus> statuses = provider.getFeedStatuses();
            SubscriptionFeedStatus mqttFeed = (SubscriptionFeedStatus) statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(ConnectionState.STALE, mqttFeed.connectionState());
            assertEquals(HealthStatus.DEGRADED, mqttFeed.healthStatus());
        }

        @Test
        @DisplayName("PSKReporter MQTT reports DISCONNECTED when not connected")
        void testGetFeedStatuses_NotConnected_ReportsDisconnected() {
            when(scheduler.getScheduledExecutions()).thenReturn(List.of());
            when(spotSource.isConnected()).thenReturn(false);

            List<FeedStatus> statuses = provider.getFeedStatuses();
            SubscriptionFeedStatus mqttFeed = (SubscriptionFeedStatus) statuses.stream()
                    .filter(f -> FEED_PSKREPORTER_MQTT.equals(f.name()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(ConnectionState.DISCONNECTED, mqttFeed.connectionState());
            assertEquals(HealthStatus.UNHEALTHY, mqttFeed.healthStatus());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("returns notScheduledFeed for PSKReporter MQTT")
        void testTriggerRefresh_PskReporterFeed_ReturnsNotScheduledFeed() {
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(FEED_PSKREPORTER_MQTT);

            assertTrue(result.isPresent());
            assertFalse(result.get().success());
            assertEquals(FEED_PSKREPORTER_MQTT, result.get().feedName());
            assertEquals("Cannot trigger refresh for subscription feeds", result.get().message());
        }
    }
}
