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
 * Unit tests for {@link BandActivityChangedEvent}.
 */
class BandActivityChangedEventTest {

    private static final Instant NOW = Instant.parse("2025-01-15T12:00:00Z");
    private static final String BAND_20M = "20m";
    private static final String BAND_40M = "40m";
    private static final String BAND_15M = "15m";
    private static final String BAND_160M = "160m";

    // =========================================================================
    // Constructor Tests
    // =========================================================================

    @Nested
    class ConstructorTests {

        @Test
        void testConstructor_NullMap_BecomesEmptyMap() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(null);

            assertThat(event.bandActivities()).isEmpty();
        }

        @Test
        void testConstructor_DefensiveCopy_MapNotModifiable() {
            Map<String, BandActivity> mutableMap = new HashMap<>();
            mutableMap.put(BAND_20M, createBandActivity(BAND_20M, 100, true));

            BandActivityChangedEvent event = new BandActivityChangedEvent(mutableMap);

            // Modify original
            mutableMap.put(BAND_40M, createBandActivity(BAND_40M, 50, false));

            // Event should not be affected
            assertThat(event.bandActivities()).hasSize(1);
            assertThat(event.bandActivities()).containsKey(BAND_20M);
            assertThat(event.bandActivities()).doesNotContainKey(BAND_40M);
        }
    }

    // =========================================================================
    // GetHotBands Tests
    // =========================================================================

    @Nested
    class GetHotBandsTests {

        @Test
        void testGetHotBands_MultipleFavorableBands_ReturnsAll() {
            // Note: isFavorable requires spotCount >= 100, trend > 0, and non-empty paths
            Map<String, BandActivity> activities = Map.of(
                    BAND_20M, createBandActivity(BAND_20M, 150, true),
                    BAND_15M, createBandActivity(BAND_15M, 200, true),
                    BAND_40M, createBandActivity(BAND_40M, 50, false)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.getHotBands()).containsExactlyInAnyOrder(BAND_20M, BAND_15M);
        }

        @Test
        void testGetHotBands_NoFavorableBands_ReturnsEmpty() {
            Map<String, BandActivity> activities = Map.of(
                    BAND_20M, createBandActivity(BAND_20M, 50, false),
                    BAND_40M, createBandActivity(BAND_40M, 30, false)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.getHotBands()).isEmpty();
        }

        @Test
        void testGetHotBands_EmptyMap_ReturnsEmpty() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(Map.of());

            assertThat(event.getHotBands()).isEmpty();
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
                    BAND_20M, createBandActivity(BAND_20M, 100, true),
                    BAND_40M, createBandActivity(BAND_40M, 50, false),
                    BAND_15M, createBandActivity(BAND_15M, 75, false)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            // 100 + 50 + 75 = 225
            assertThat(event.getTotalSpotCount()).isEqualTo(225);
        }

        @Test
        void testGetTotalSpotCount_EmptyMap_ReturnsZero() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(Map.of());

            assertThat(event.getTotalSpotCount()).isZero();
        }

        @Test
        void testGetTotalSpotCount_SingleBand_ReturnsCount() {
            Map<String, BandActivity> activities = Map.of(
                    BAND_20M, createBandActivity(BAND_20M, 150, true)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.getTotalSpotCount()).isEqualTo(150);
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
                    BAND_20M, createBandActivity(BAND_20M, 100, true),
                    BAND_40M, createBandActivity(BAND_40M, 50, false),
                    BAND_15M, createBandActivity(BAND_15M, 75, true)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.getBandCount()).isEqualTo(3);
        }

        @Test
        void testGetBandCount_EmptyMap_ReturnsZero() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(Map.of());

            assertThat(event.getBandCount()).isZero();
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
                    BAND_20M, createBandActivity(BAND_20M, 100, true)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.hasActivity()).isTrue();
        }

        @Test
        void testHasActivity_EmptyMap_ReturnsFalse() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(Map.of());

            assertThat(event.hasActivity()).isFalse();
        }

        @Test
        void testHasActivity_NullMap_ReturnsFalse() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(null);

            assertThat(event.hasActivity()).isFalse();
        }
    }

    // =========================================================================
    // GetActivity Tests
    // =========================================================================

    @Nested
    class GetActivityTests {

        @Test
        void testGetActivity_ExistingBand_ReturnsActivity() {
            BandActivity expected = createBandActivity(BAND_20M, 100, true);
            Map<String, BandActivity> activities = Map.of(BAND_20M, expected);

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.getActivity(BAND_20M)).isEqualTo(expected);
        }

        @Test
        void testGetActivity_NonExistentBand_ReturnsNull() {
            Map<String, BandActivity> activities = Map.of(
                    BAND_20M, createBandActivity(BAND_20M, 100, true)
            );

            BandActivityChangedEvent event = new BandActivityChangedEvent(activities);

            assertThat(event.getActivity(BAND_160M)).isNull();
        }

        @Test
        void testGetActivity_EmptyMap_ReturnsNull() {
            BandActivityChangedEvent event = new BandActivityChangedEvent(Map.of());

            assertThat(event.getActivity(BAND_20M)).isNull();
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private BandActivity createBandActivity(String band, int spotCount, boolean favorable) {
        // For favorable: need spotCount >= 100, trend > 0, and non-empty paths
        double trend = favorable ? 25.0 : -5.0;
        Set<ContinentPath> paths = favorable
                ? Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS)
                : Set.of();

        return new BandActivity(
                band,
                "FT8",
                spotCount,
                80,
                trend,
                10000,
                "JA1ABC → W6XYZ",
                paths,
                NOW.minusSeconds(900),
                NOW,
                NOW
        );
    }
}
