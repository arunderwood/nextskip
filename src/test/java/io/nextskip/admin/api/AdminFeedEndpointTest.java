package io.nextskip.admin.api;

import io.nextskip.common.admin.AdminStatusProvider;
import io.nextskip.common.admin.ConnectionState;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.SubscriptionFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static io.nextskip.test.TestConstants.FEED_BAND_ACTIVITY;
import static io.nextskip.test.TestConstants.FEED_HAMQSL_BAND;
import static io.nextskip.test.TestConstants.FEED_HAMQSL_SOLAR;
import static io.nextskip.test.TestConstants.FEED_NOAA_SWPC;
import static io.nextskip.test.TestConstants.FEED_POTA;
import static io.nextskip.test.TestConstants.FEED_PSKREPORTER_MQTT;
import static io.nextskip.test.TestConstants.FEED_UNKNOWN;
import static io.nextskip.test.TestConstants.MODULE_ACTIVATIONS;
import static io.nextskip.test.TestConstants.MODULE_PROPAGATION;
import static io.nextskip.test.TestConstants.MODULE_SPOTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminFeedEndpoint}.
 *
 * <p>Tests the Hilla endpoint that aggregates feed status from all modules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminFeedEndpoint")
class AdminFeedEndpointTest {

    @Mock
    private AdminStatusProvider propagationProvider;

    @Mock
    private AdminStatusProvider activationsProvider;

    @Mock
    private AdminStatusProvider spotsProvider;

    private AdminFeedEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new AdminFeedEndpoint(List.of(propagationProvider, activationsProvider, spotsProvider));
    }

    @Nested
    @DisplayName("getFeedStatuses")
    class GetFeedStatusesTests {

        @Test
        @DisplayName("aggregates statuses from all providers")
        void testGetFeedStatuses_MultipleProviders_AggregatesAll() {
            // Given
            when(propagationProvider.getModuleName()).thenReturn(MODULE_PROPAGATION);
            when(propagationProvider.getFeedStatuses()).thenReturn(List.of(
                    createScheduledFeedStatus(FEED_NOAA_SWPC),
                    createScheduledFeedStatus(FEED_HAMQSL_SOLAR)
            ));

            when(activationsProvider.getModuleName()).thenReturn(MODULE_ACTIVATIONS);
            when(activationsProvider.getFeedStatuses()).thenReturn(List.of(
                    createScheduledFeedStatus(FEED_POTA)
            ));

            when(spotsProvider.getModuleName()).thenReturn(MODULE_SPOTS);
            when(spotsProvider.getFeedStatuses()).thenReturn(List.of(
                    createSubscriptionFeedStatus(FEED_PSKREPORTER_MQTT),
                    createScheduledFeedStatus(FEED_BAND_ACTIVITY)
            ));

            // When
            FeedStatusResponse response = endpoint.getFeedStatuses();

            // Then
            assertNotNull(response);
            assertEquals(3, response.modules().size());
            assertEquals(5, response.totalFeeds());
            assertNotNull(response.timestamp());
        }

        @Test
        @DisplayName("groups feeds by module")
        void testGetFeedStatuses_GroupsByModule() {
            // Given
            when(propagationProvider.getModuleName()).thenReturn(MODULE_PROPAGATION);
            when(propagationProvider.getFeedStatuses()).thenReturn(List.of(
                    createScheduledFeedStatus(FEED_NOAA_SWPC),
                    createScheduledFeedStatus(FEED_HAMQSL_SOLAR),
                    createScheduledFeedStatus(FEED_HAMQSL_BAND)
            ));

            when(activationsProvider.getModuleName()).thenReturn(MODULE_ACTIVATIONS);
            when(activationsProvider.getFeedStatuses()).thenReturn(List.of());

            when(spotsProvider.getModuleName()).thenReturn(MODULE_SPOTS);
            when(spotsProvider.getFeedStatuses()).thenReturn(List.of());

            // When
            FeedStatusResponse response = endpoint.getFeedStatuses();

            // Then
            ModuleFeedStatus propagationModule = response.modules().stream()
                    .filter(m -> MODULE_PROPAGATION.equals(m.moduleName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(3, propagationModule.scheduledFeeds().size());
            assertEquals(0, propagationModule.subscriptionFeeds().size());
        }

        @Test
        @DisplayName("separates scheduled and subscription feeds")
        void testGetFeedStatuses_SeparatesFeedTypes() {
            // Given
            when(spotsProvider.getModuleName()).thenReturn(MODULE_SPOTS);
            when(spotsProvider.getFeedStatuses()).thenReturn(List.of(
                    createSubscriptionFeedStatus(FEED_PSKREPORTER_MQTT),
                    createScheduledFeedStatus(FEED_BAND_ACTIVITY)
            ));

            when(propagationProvider.getModuleName()).thenReturn(MODULE_PROPAGATION);
            when(propagationProvider.getFeedStatuses()).thenReturn(List.of());

            when(activationsProvider.getModuleName()).thenReturn(MODULE_ACTIVATIONS);
            when(activationsProvider.getFeedStatuses()).thenReturn(List.of());

            // When
            FeedStatusResponse response = endpoint.getFeedStatuses();

            // Then
            ModuleFeedStatus spotsModule = response.modules().stream()
                    .filter(m -> MODULE_SPOTS.equals(m.moduleName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(1, spotsModule.scheduledFeeds().size());
            assertEquals(1, spotsModule.subscriptionFeeds().size());
            assertEquals(FEED_BAND_ACTIVITY, spotsModule.scheduledFeeds().get(0).name());
            assertEquals(FEED_PSKREPORTER_MQTT, spotsModule.subscriptionFeeds().get(0).name());
        }

        @Test
        @DisplayName("handles empty provider list")
        void testGetFeedStatuses_NoProviders_ReturnsEmptyResponse() {
            // Given
            endpoint = new AdminFeedEndpoint(List.of());

            // When
            FeedStatusResponse response = endpoint.getFeedStatuses();

            // Then
            assertNotNull(response);
            assertEquals(0, response.modules().size());
            assertEquals(0, response.totalFeeds());
        }

        @Test
        @DisplayName("calculates correct total feed count")
        void testGetFeedStatuses_CalculatesTotalFeeds() {
            // Given
            when(propagationProvider.getModuleName()).thenReturn(MODULE_PROPAGATION);
            when(propagationProvider.getFeedStatuses()).thenReturn(List.of(
                    createScheduledFeedStatus("Feed1"),
                    createScheduledFeedStatus("Feed2")
            ));

            when(activationsProvider.getModuleName()).thenReturn(MODULE_ACTIVATIONS);
            when(activationsProvider.getFeedStatuses()).thenReturn(List.of(
                    createScheduledFeedStatus("Feed3")
            ));

            when(spotsProvider.getModuleName()).thenReturn(MODULE_SPOTS);
            when(spotsProvider.getFeedStatuses()).thenReturn(List.of(
                    createSubscriptionFeedStatus("Feed4"),
                    createScheduledFeedStatus("Feed5")
            ));

            // When
            FeedStatusResponse response = endpoint.getFeedStatuses();

            // Then
            assertEquals(5, response.totalFeeds());
        }
    }

    @Nested
    @DisplayName("triggerRefresh")
    class TriggerRefreshTests {

        @Test
        @DisplayName("delegates to provider that handles the feed")
        void testTriggerRefresh_KnownFeed_DelegatesToProvider() {
            // Given
            Instant now = Instant.now();
            when(propagationProvider.triggerRefresh(FEED_NOAA_SWPC))
                    .thenReturn(Optional.of(TriggerRefreshResult.success(FEED_NOAA_SWPC, now)));

            // When
            TriggerRefreshResult result = endpoint.triggerRefresh(FEED_NOAA_SWPC);

            // Then
            assertTrue(result.success());
            assertEquals(FEED_NOAA_SWPC, result.feedName());
            assertEquals(now, result.scheduledFor());
        }

        @Test
        @DisplayName("searches providers until one handles the feed")
        void testTriggerRefresh_SecondProvider_FindsFeed() {
            // Given
            Instant now = Instant.now();
            when(propagationProvider.triggerRefresh(FEED_POTA)).thenReturn(Optional.empty());
            when(activationsProvider.triggerRefresh(FEED_POTA))
                    .thenReturn(Optional.of(TriggerRefreshResult.success(FEED_POTA, now)));

            // When
            TriggerRefreshResult result = endpoint.triggerRefresh(FEED_POTA);

            // Then
            assertTrue(result.success());
            assertEquals(FEED_POTA, result.feedName());
        }

        @Test
        @DisplayName("returns unknownFeed when no provider handles feed")
        void testTriggerRefresh_UnknownFeed_ReturnsUnknownFeed() {
            // Given
            when(propagationProvider.triggerRefresh(FEED_UNKNOWN)).thenReturn(Optional.empty());
            when(activationsProvider.triggerRefresh(FEED_UNKNOWN)).thenReturn(Optional.empty());
            when(spotsProvider.triggerRefresh(FEED_UNKNOWN)).thenReturn(Optional.empty());

            // When
            TriggerRefreshResult result = endpoint.triggerRefresh(FEED_UNKNOWN);

            // Then
            assertFalse(result.success());
            assertEquals(FEED_UNKNOWN, result.feedName());
            assertEquals("Unknown feed: " + FEED_UNKNOWN, result.message());
        }

        @Test
        @DisplayName("returns notScheduledFeed for subscription feeds")
        void testTriggerRefresh_SubscriptionFeed_ReturnsNotScheduled() {
            // Given
            when(propagationProvider.triggerRefresh(FEED_PSKREPORTER_MQTT)).thenReturn(Optional.empty());
            when(activationsProvider.triggerRefresh(FEED_PSKREPORTER_MQTT)).thenReturn(Optional.empty());
            when(spotsProvider.triggerRefresh(FEED_PSKREPORTER_MQTT))
                    .thenReturn(Optional.of(TriggerRefreshResult.notScheduledFeed(FEED_PSKREPORTER_MQTT)));

            // When
            TriggerRefreshResult result = endpoint.triggerRefresh(FEED_PSKREPORTER_MQTT);

            // Then
            assertFalse(result.success());
            assertEquals(FEED_PSKREPORTER_MQTT, result.feedName());
            assertEquals("Cannot trigger refresh for subscription feeds", result.message());
        }

        @Test
        @DisplayName("returns failure result from provider")
        void testTriggerRefresh_ProviderFailure_ReturnsFailure() {
            // Given
            when(propagationProvider.triggerRefresh(FEED_NOAA_SWPC))
                    .thenReturn(Optional.of(new TriggerRefreshResult(
                            false,
                            "Failed to trigger refresh: Database error",
                            FEED_NOAA_SWPC,
                            null
                    )));

            // When
            TriggerRefreshResult result = endpoint.triggerRefresh(FEED_NOAA_SWPC);

            // Then
            assertFalse(result.success());
            assertEquals(FEED_NOAA_SWPC, result.feedName());
            assertTrue(result.message().contains("Database error"));
        }
    }

    private ScheduledFeedStatus createScheduledFeedStatus(String name) {
        return ScheduledFeedStatus.of(
                name,
                Instant.now().minusSeconds(300),
                Instant.now().plusSeconds(300),
                false,
                0,
                null,
                300
        );
    }

    private SubscriptionFeedStatus createSubscriptionFeedStatus(String name) {
        return SubscriptionFeedStatus.of(
                name,
                ConnectionState.CONNECTED,
                Instant.now(),
                0
        );
    }
}
