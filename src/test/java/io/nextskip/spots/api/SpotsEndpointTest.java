package io.nextskip.spots.api;

import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.model.ContinentPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpotsEndpoint}.
 *
 * <p>Tests the browser-callable endpoint methods for the React dashboard.
 */
@ExtendWith(MockitoExtension.class)
class SpotsEndpointTest {

    private static final Instant NOW = Instant.parse("2025-01-15T12:00:00Z");
    private static final String BAND_20M = "20m";

    @Mock
    private SpotsService spotsService;

    private SpotsEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new SpotsEndpoint(spotsService);
    }

    // =========================================================================
    // getBandActivity() Tests
    // =========================================================================

    @Nested
    class GetBandActivityTests {

        @Test
        void testGetBandActivity_ReturnsBandActivityResponse() {
            // Given
            BandActivityResponse expected = new BandActivityResponse(
                    Map.of(BAND_20M, createBandActivity()),
                    NOW,
                    true
            );
            when(spotsService.getBandActivityResponse()).thenReturn(expected);

            // When
            BandActivityResponse result = endpoint.getBandActivity();

            // Then
            assertThat(result).isEqualTo(expected);
            verify(spotsService).getBandActivityResponse();
        }

        @Test
        void testGetBandActivity_EmptyActivities_ReturnsEmptyMap() {
            // Given
            BandActivityResponse expected = new BandActivityResponse(
                    Map.of(),
                    NOW,
                    false
            );
            when(spotsService.getBandActivityResponse()).thenReturn(expected);

            // When
            BandActivityResponse result = endpoint.getBandActivity();

            // Then
            assertThat(result.bandActivities()).isEmpty();
            assertThat(result.mqttConnected()).isFalse();
        }
    }

    // =========================================================================
    // getBandActivityForBand() Tests
    // =========================================================================

    @Nested
    class GetBandActivityForBandTests {

        @Test
        void testGetBandActivityForBand_ExistingBand_ReturnsActivity() {
            // Given
            BandActivity expected = createBandActivity();
            when(spotsService.getBandActivity(BAND_20M)).thenReturn(Optional.of(expected));

            // When
            BandActivity result = endpoint.getBandActivityForBand(BAND_20M);

            // Then
            assertThat(result).isEqualTo(expected);
            verify(spotsService).getBandActivity(BAND_20M);
        }

        @Test
        void testGetBandActivityForBand_NonExistentBand_ReturnsNull() {
            // Given
            when(spotsService.getBandActivity("160m")).thenReturn(Optional.empty());

            // When
            BandActivity result = endpoint.getBandActivityForBand("160m");

            // Then
            assertThat(result).isNull();
        }
    }

    // =========================================================================
    // getStatus() Tests
    // =========================================================================

    @Nested
    class GetStatusTests {

        @Test
        void testGetStatus_Connected_ReturnsFullStatus() {
            // Given
            when(spotsService.isConnected()).thenReturn(true);
            when(spotsService.getSourceName()).thenReturn("PSKReporter MQTT");
            when(spotsService.getSpotCount()).thenReturn(12345L);
            when(spotsService.getLastSpotTime()).thenReturn(Optional.of(NOW));
            when(spotsService.getSpotsProcessed()).thenReturn(99999L);

            // When
            SpotsStatusResponse result = endpoint.getStatus();

            // Then
            assertThat(result.connected()).isTrue();
            assertThat(result.sourceName()).isEqualTo("PSKReporter MQTT");
            assertThat(result.totalSpots()).isEqualTo(12345L);
            assertThat(result.lastSpotTime()).isEqualTo(NOW);
            assertThat(result.spotsProcessed()).isEqualTo(99999L);
        }

        @Test
        void testGetStatus_Disconnected_ReturnsStatus() {
            // Given
            when(spotsService.isConnected()).thenReturn(false);
            when(spotsService.getSourceName()).thenReturn("PSKReporter MQTT");
            when(spotsService.getSpotCount()).thenReturn(0L);
            when(spotsService.getLastSpotTime()).thenReturn(Optional.empty());
            when(spotsService.getSpotsProcessed()).thenReturn(0L);

            // When
            SpotsStatusResponse result = endpoint.getStatus();

            // Then
            assertThat(result.connected()).isFalse();
            assertThat(result.lastSpotTime()).isNull();
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private BandActivity createBandActivity() {
        return new BandActivity(
                BAND_20M,
                "FT8",
                100,
                80,
                25.0,
                10000,
                "JA1ABC → W6XYZ",
                Set.of(ContinentPath.NA_AS),
                NOW.minusSeconds(900),
                NOW,
                NOW
        );
    }
}
