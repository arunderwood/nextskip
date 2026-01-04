package io.nextskip.spots.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpotsStatusResponse}.
 */
class SpotsStatusResponseTest {

    private static final Instant NOW = Instant.parse("2025-01-15T12:00:00Z");
    private static final String SOURCE_NAME = "PSKReporter MQTT";

    // =========================================================================
    // IsHealthy Tests
    // =========================================================================

    @Nested
    class IsHealthyTests {

        @Test
        void testIsHealthy_ConnectedWithSpots_ReturnsTrue() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true,
                    SOURCE_NAME,
                    1000L,
                    NOW,
                    5000L
            );

            assertThat(response.isHealthy()).isTrue();
        }

        @Test
        void testIsHealthy_DisconnectedWithSpots_ReturnsFalse() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    false,
                    SOURCE_NAME,
                    1000L,
                    NOW,
                    5000L
            );

            assertThat(response.isHealthy()).isFalse();
        }

        @Test
        void testIsHealthy_ConnectedNoSpots_ReturnsFalse() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true,
                    SOURCE_NAME,
                    0L,
                    null,
                    0L
            );

            assertThat(response.isHealthy()).isFalse();
        }

        @Test
        void testIsHealthy_DisconnectedNoSpots_ReturnsFalse() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    false,
                    SOURCE_NAME,
                    0L,
                    null,
                    0L
            );

            assertThat(response.isHealthy()).isFalse();
        }
    }

    // =========================================================================
    // GetStatusDescription Tests
    // =========================================================================

    @Nested
    class GetStatusDescriptionTests {

        @Test
        void testGetStatusDescription_Disconnected_ReturnsDisconnectedMessage() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    false,
                    SOURCE_NAME,
                    1000L,
                    NOW,
                    5000L
            );

            assertThat(response.getStatusDescription())
                    .isEqualTo("Disconnected from PSKReporter MQTT");
        }

        @Test
        void testGetStatusDescription_ConnectedNoSpots_ReturnsWaitingMessage() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true,
                    SOURCE_NAME,
                    0L,
                    null,
                    0L
            );

            assertThat(response.getStatusDescription())
                    .isEqualTo("Connected, waiting for first spot");
        }

        @Test
        void testGetStatusDescription_ConnectedWithSpots_ReturnsConnectedMessage() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true,
                    SOURCE_NAME,
                    1234L,
                    NOW,
                    5000L
            );

            assertThat(response.getStatusDescription())
                    .isEqualTo("Connected, 1234 spots in database");
        }
    }

    // =========================================================================
    // Record Accessor Tests
    // =========================================================================

    @Nested
    class AccessorTests {

        @Test
        void testConnected_ReturnsConnectionStatus() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true, SOURCE_NAME, 100L, NOW, 500L);

            assertThat(response.connected()).isTrue();
        }

        @Test
        void testSourceName_ReturnsSourceName() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true, SOURCE_NAME, 100L, NOW, 500L);

            assertThat(response.sourceName()).isEqualTo(SOURCE_NAME);
        }

        @Test
        void testTotalSpots_ReturnsTotalSpots() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true, SOURCE_NAME, 100L, NOW, 500L);

            assertThat(response.totalSpots()).isEqualTo(100L);
        }

        @Test
        void testLastSpotTime_ReturnsLastSpotTime() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true, SOURCE_NAME, 100L, NOW, 500L);

            assertThat(response.lastSpotTime()).isEqualTo(NOW);
        }

        @Test
        void testSpotsProcessed_ReturnsSpotsProcessed() {
            SpotsStatusResponse response = new SpotsStatusResponse(
                    true, SOURCE_NAME, 100L, NOW, 500L);

            assertThat(response.spotsProcessed()).isEqualTo(500L);
        }
    }
}
