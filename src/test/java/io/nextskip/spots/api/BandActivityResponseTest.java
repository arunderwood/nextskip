package io.nextskip.spots.api;

import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.model.ContinentPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BandActivityResponse}.
 */
class BandActivityResponseTest {

    private static final Instant NOW = Instant.parse("2025-01-15T12:00:00Z");

    // =========================================================================
    // Constructor Tests
    // =========================================================================

    @Nested
    class ConstructorTests {

        @Test
        void testConstructor_FullValues_SetsAllFields() {
            Map<String, BandActivity> activities = Map.of(
                    "20m", createBandActivity("20m", 100)
            );

            BandActivityResponse response = new BandActivityResponse(activities, NOW, true);

            assertThat(response.bandActivities()).hasSize(1);
            assertThat(response.timestamp()).isEqualTo(NOW);
            assertThat(response.mqttConnected()).isTrue();
        }

        @Test
        void testConstructor_NullMap_BecomesEmptyMap() {
            BandActivityResponse response = new BandActivityResponse(null, NOW, false);

            assertThat(response.bandActivities()).isEmpty();
        }

        @Test
        void testConstructor_DefensiveCopy_MapNotModifiable() {
            Map<String, BandActivity> mutableMap = new HashMap<>();
            mutableMap.put("20m", createBandActivity("20m", 100));

            BandActivityResponse response = new BandActivityResponse(mutableMap, NOW, true);

            // Modify original
            mutableMap.put("40m", createBandActivity("40m", 50));

            // Response should not be affected
            assertThat(response.bandActivities()).hasSize(1);
            assertThat(response.bandActivities()).containsKey("20m");
            assertThat(response.bandActivities()).doesNotContainKey("40m");
        }
    }

    // =========================================================================
    // GetBandCount Tests
    // =========================================================================

    @Nested
    class GetBandCountTests {

        @Test
        void testGetBandCount_MultipleBands_ReturnsCount() {
            Map<String, BandActivity> activities = Map.of(
                    "20m", createBandActivity("20m", 100),
                    "40m", createBandActivity("40m", 50),
                    "15m", createBandActivity("15m", 75)
            );

            BandActivityResponse response = new BandActivityResponse(activities, NOW, true);

            assertThat(response.getBandCount()).isEqualTo(3);
        }

        @Test
        void testGetBandCount_EmptyMap_ReturnsZero() {
            BandActivityResponse response = new BandActivityResponse(Map.of(), NOW, true);

            assertThat(response.getBandCount()).isZero();
        }
    }

    // =========================================================================
    // GetTotalSpotCount Tests
    // =========================================================================

    @Nested
    class GetTotalSpotCountTests {

        @Test
        void testGetTotalSpotCount_MultipleBands_SumsAllCounts() {
            Map<String, BandActivity> activities = Map.of(
                    "20m", createBandActivity("20m", 100),
                    "40m", createBandActivity("40m", 50),
                    "15m", createBandActivity("15m", 75)
            );

            BandActivityResponse response = new BandActivityResponse(activities, NOW, true);

            assertThat(response.getTotalSpotCount()).isEqualTo(225);
        }

        @Test
        void testGetTotalSpotCount_EmptyMap_ReturnsZero() {
            BandActivityResponse response = new BandActivityResponse(Map.of(), NOW, true);

            assertThat(response.getTotalSpotCount()).isZero();
        }

        @Test
        void testGetTotalSpotCount_SingleBand_ReturnsCount() {
            Map<String, BandActivity> activities = Map.of(
                    "20m", createBandActivity("20m", 150)
            );

            BandActivityResponse response = new BandActivityResponse(activities, NOW, true);

            assertThat(response.getTotalSpotCount()).isEqualTo(150);
        }
    }

    // =========================================================================
    // HasActivity Tests
    // =========================================================================

    @Nested
    class HasActivityTests {

        @Test
        void testHasActivity_WithBands_ReturnsTrue() {
            Map<String, BandActivity> activities = Map.of(
                    "20m", createBandActivity("20m", 100)
            );

            BandActivityResponse response = new BandActivityResponse(activities, NOW, true);

            assertThat(response.hasActivity()).isTrue();
        }

        @Test
        void testHasActivity_EmptyMap_ReturnsFalse() {
            BandActivityResponse response = new BandActivityResponse(Map.of(), NOW, true);

            assertThat(response.hasActivity()).isFalse();
        }

        @Test
        void testHasActivity_NullMap_ReturnsFalse() {
            BandActivityResponse response = new BandActivityResponse(null, NOW, true);

            assertThat(response.hasActivity()).isFalse();
        }
    }

    // =========================================================================
    // Record Accessor Tests
    // =========================================================================

    @Nested
    class AccessorTests {

        @Test
        void testBandActivities_ReturnsBandActivitiesMap() {
            Map<String, BandActivity> activities = Map.of(
                    "20m", createBandActivity("20m", 100)
            );

            BandActivityResponse response = new BandActivityResponse(activities, NOW, true);

            assertThat(response.bandActivities()).hasSize(1);
        }

        @Test
        void testMqttConnected_ReturnsConnectionStatus() {
            BandActivityResponse response = new BandActivityResponse(Map.of(), NOW, true);

            assertThat(response.mqttConnected()).isTrue();
        }

        @Test
        void testTimestamp_ReturnsTimestamp() {
            BandActivityResponse response = new BandActivityResponse(Map.of(), NOW, true);

            assertThat(response.timestamp()).isEqualTo(NOW);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private BandActivity createBandActivity(String band, int spotCount) {
        return new BandActivity(
                band,
                "FT8",
                spotCount,
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
