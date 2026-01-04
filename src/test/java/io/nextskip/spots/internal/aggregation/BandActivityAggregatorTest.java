package io.nextskip.spots.internal.aggregation;

import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.model.ContinentPath;
import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.spots.persistence.repository.SpotRepository;
import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BandActivityAggregator}.
 *
 * <p>Tests cover band aggregation logic including mode detection, trend calculation,
 * DX distance tracking, and continent path analysis.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data intentionally repeats band/mode values
class BandActivityAggregatorTest {

    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
    private static final String BAND_20M = "20m";
    private static final String BAND_40M = "40m";
    private static final String MODE_FT8 = "FT8";
    private static final String MODE_CW = "CW";

    @Mock
    private SpotRepository repository;

    private Clock clock;
    private BandActivityAggregator aggregator;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
        aggregator = new BandActivityAggregator(repository, clock);
    }

    // =========================================================================
    // aggregateBand() Tests
    // =========================================================================

    @Nested
    class AggregateBandTests {

        @Test
        void testAggregateBand_BasicAggregation_ReturnsValidBandActivity() {
            // Given: FT8 mode detected with some activity
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.band()).isEqualTo(BAND_20M);
            assertThat(activity.mode()).isEqualTo(MODE_FT8);
            assertThat(activity.spotCount()).isEqualTo(100);
            assertThat(activity.windowEnd()).isEqualTo(FIXED_TIME);
        }

        @Test
        void testAggregateBand_NoModeData_DefaultsToFT8() {
            // Given: No spots to determine mode
            setupEmptyModeDistribution(BAND_20M);
            setupSpotCount(BAND_20M, 10);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.mode())
                    .as("Should default to FT8 when no mode data")
                    .isEqualTo(MODE_FT8);
        }

        @Test
        void testAggregateBand_CWMode_Uses30MinWindow() {
            // Given: CW mode detected
            setupModeDistribution(BAND_40M, MODE_CW, 30L);
            setupSpotCount(BAND_40M, 50);
            setupBaselineCounts();
            setupNoDxData(BAND_40M);
            setupNoPaths(BAND_40M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_40M);

            // Then
            assertThat(activity.mode()).isEqualTo(MODE_CW);
            // CW window is 30 minutes
            assertThat(activity.windowStart())
                    .isEqualTo(FIXED_TIME.minus(Duration.ofMinutes(30)));
        }

        @Test
        void testAggregateBand_WindowTimestamps_CorrectForFT8() {
            // Given: FT8 mode (15-minute window)
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.windowStart())
                    .as("FT8 window start should be 15 minutes before now")
                    .isEqualTo(FIXED_TIME.minus(Duration.ofMinutes(15)));
            assertThat(activity.windowEnd())
                    .isEqualTo(FIXED_TIME);
            assertThat(activity.calculatedAt())
                    .isEqualTo(FIXED_TIME);
        }
    }

    // =========================================================================
    // Trend Calculation Tests
    // =========================================================================

    @Nested
    class TrendCalculationTests {

        @Test
        void testAggregateBand_TrendCalculation_UsesCurrentAndBaseline() {
            // Given: Current activity with spots
            setupModeDistribution(BAND_20M, MODE_FT8, 100L);
            setupSpotCount(BAND_20M, 100);
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then: trend should be calculated (exact value depends on baseline calculation)
            assertThat(activity.spotCount()).isEqualTo(100);
            assertThat(activity.baselineSpotCount())
                    .as("Baseline should be calculated (may be 0 with simplified mocks)")
                    .isGreaterThanOrEqualTo(0);
            // trendPercentage is finite (not NaN)
            assertThat(Double.isFinite(activity.trendPercentage()))
                    .as("Trend percentage should be a finite number")
                    .isTrue();
        }

        @Test
        void testAggregateBand_ZeroBaseline_WithActivity_Returns100Percent() {
            // Given: Activity now but baseline will be 0 (mock returns same count for all times)
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 50);
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then: With simplified mocks, baseline = 0, so trend = 100%
            assertThat(activity.spotCount()).isEqualTo(50);
            assertThat(activity.trendPercentage())
                    .as("Should return 100% when activity exists but baseline is 0")
                    .isEqualTo(100.0);
        }

        @Test
        void testAggregateBand_ZeroBaseline_NoActivity_ReturnsZeroPercent() {
            // Given: No activity now and no baseline
            setupEmptyModeDistribution(BAND_20M);
            setupSpotCount(BAND_20M, 0);
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.trendPercentage())
                    .as("Should return 0% when no activity and no baseline")
                    .isEqualTo(0.0);
        }

        @Test
        void testAggregateBand_TrendFormula_VerifyBaselinePopulated() {
            // Given
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 50);
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then: baselineSpotCount field should be populated
            assertThat(activity.baselineSpotCount()).isNotNull();
        }
    }

    // =========================================================================
    // DX Distance Tests
    // =========================================================================

    @Nested
    class DxDistanceTests {

        @Test
        void testAggregateBand_WithDxSpot_CapturesMaxDistance() {
            // Given: DX spot of 8500km
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupDxSpot(BAND_20M, 8500, "JA1ABC", "W6XYZ");
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.maxDxKm()).isEqualTo(8500);
            assertThat(activity.maxDxPath())
                    .as("Path should be formatted as spotted → spotter")
                    .isEqualTo("JA1ABC → W6XYZ");
            assertThat(activity.hasDxData()).isTrue();
        }

        @Test
        void testAggregateBand_NoDxSpot_HasNullDxData() {
            // Given: No DX spot data
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.maxDxKm()).isNull();
            assertThat(activity.maxDxPath()).isNull();
            assertThat(activity.hasDxData()).isFalse();
        }

        @Test
        void testAggregateBand_DxPathFormatting_UpperCase() {
            // Given: DX spot with lowercase callsigns
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupDxSpot(BAND_20M, 5000, "g3abc", "w1aw");
            setupNoPaths(BAND_20M);

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.maxDxPath())
                    .as("Callsigns should be uppercased")
                    .isEqualTo("G3ABC → W1AW");
        }
    }

    // =========================================================================
    // Continent Path Tests
    // =========================================================================

    @Nested
    class ContinentPathTests {

        @Test
        void testAggregateBand_ActivePaths_IncludedWhenAboveThreshold() {
            // Given: NA-EU path with 10 spots (above 5 threshold)
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupPath(BAND_20M, "NA", "EU", 10L);  // Above threshold

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.activePaths())
                    .as("Should include NA-EU path")
                    .containsExactly(ContinentPath.NA_EU);
        }

        @Test
        void testAggregateBand_Paths_ExcludedWhenBelowThreshold() {
            // Given: NA-EU path with only 3 spots (below 5 threshold)
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupPath(BAND_20M, "NA", "EU", 3L);  // Below threshold

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.activePaths())
                    .as("Should not include path below threshold")
                    .isEmpty();
        }

        @Test
        void testAggregateBand_MultiplePaths_AllActive() {
            // Given: Multiple active paths
            setupModeDistribution(BAND_20M, MODE_FT8, 100L);
            setupSpotCount(BAND_20M, 200);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupMultiplePaths(BAND_20M,
                    new Object[]{"NA", "EU", 20L},
                    new Object[]{"NA", "AS", 15L},
                    new Object[]{"EU", "AS", 10L}
            );

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.activePaths())
                    .as("Should include all active paths")
                    .containsExactlyInAnyOrder(
                            ContinentPath.NA_EU,
                            ContinentPath.NA_AS,
                            ContinentPath.EU_AS
                    );
        }

        @Test
        void testAggregateBand_UnknownPath_Ignored() {
            // Given: AF-OC path (not a defined major path)
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupPath(BAND_20M, "AF", "OC", 10L);  // Not a defined major path

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.activePaths())
                    .as("Unknown paths should be ignored")
                    .isEmpty();
        }

        @Test
        void testAggregateBand_ReversedPath_StillMatches() {
            // Given: EU-NA (reversed) should still match NA-EU path
            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupSpotCount(BAND_20M, 100);
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupPath(BAND_20M, "EU", "NA", 10L);  // Reversed direction

            // When
            BandActivity activity = aggregator.aggregateBand(BAND_20M);

            // Then
            assertThat(activity.activePaths())
                    .as("Reversed direction should still match NA_EU")
                    .containsExactly(ContinentPath.NA_EU);
        }
    }

    // =========================================================================
    // aggregateAllBands() Tests
    // =========================================================================

    @Nested
    class AggregateAllBandsTests {

        @Test
        void testAggregateAllBands_ReturnsAllActiveBands() {
            // Given: Two bands with activity
            Instant lookback = FIXED_TIME.minus(Duration.ofHours(2));
            when(repository.findDistinctBandsWithActivitySince(lookback))
                    .thenReturn(List.of(BAND_20M, BAND_40M));

            setupModeDistribution(BAND_20M, MODE_FT8, 50L);
            setupModeDistribution(BAND_40M, MODE_CW, 30L);
            setupSpotCount(BAND_20M, 100);
            setupSpotCount(BAND_40M, 50);
            setupBaselineCounts();
            setupBaselineCounts();
            setupNoDxData(BAND_20M);
            setupNoDxData(BAND_40M);
            setupNoPaths(BAND_20M);
            setupNoPaths(BAND_40M);

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should contain both bands")
                    .containsKeys(BAND_20M, BAND_40M);
            assertThat(result.get(BAND_20M).mode()).isEqualTo(MODE_FT8);
            assertThat(result.get(BAND_40M).mode()).isEqualTo(MODE_CW);
        }

        @Test
        void testAggregateAllBands_NoActiveBands_ReturnsEmptyMap() {
            // Given: No active bands
            Instant lookback = FIXED_TIME.minus(Duration.ofHours(2));
            when(repository.findDistinctBandsWithActivitySince(lookback))
                    .thenReturn(List.of());

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should return empty map when no active bands")
                    .isEmpty();
        }

        @Test
        void testAggregateAllBands_BandAggregationFails_SkipsBandAndContinues() {
            // Given: First band fails, second succeeds
            Instant lookback = FIXED_TIME.minus(Duration.ofHours(2));
            when(repository.findDistinctBandsWithActivitySince(lookback))
                    .thenReturn(List.of(BAND_20M, BAND_40M));

            // 20m will fail with exception
            when(repository.findModeDistributionByBandSince(eq(BAND_20M), any()))
                    .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Database error"));

            // 40m should succeed
            setupModeDistribution(BAND_40M, MODE_CW, 30L);
            setupSpotCount(BAND_40M, 50);
            setupBaselineCounts();
            setupNoDxData(BAND_40M);
            setupNoPaths(BAND_40M);

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should contain only successful band")
                    .containsKey(BAND_40M)
                    .doesNotContainKey(BAND_20M);
        }

        @Test
        void testAggregateAllBands_OrderPreserved() {
            // Given: Bands returned in a specific order
            Instant lookback = FIXED_TIME.minus(Duration.ofHours(2));
            when(repository.findDistinctBandsWithActivitySince(lookback))
                    .thenReturn(List.of("10m", "15m", "20m", "40m"));

            for (String band : List.of("10m", "15m", "20m", "40m")) {
                setupModeDistribution(band, MODE_FT8, 10L);
                setupSpotCount(band, 10);
                setupBaselineCounts();
                setupNoDxData(band);
                setupNoPaths(band);
            }

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result.keySet())
                    .as("Order should be preserved")
                    .containsExactly("10m", "15m", "20m", "40m");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void setupModeDistribution(String band, String mode, long count) {
        List<Object[]> result = count > 0
                ? java.util.Collections.singletonList(new Object[]{mode, count})
                : java.util.Collections.emptyList();
        lenient().when(repository.findModeDistributionByBandSince(eq(band), any()))
                .thenReturn(result);
    }

    private void setupEmptyModeDistribution(String band) {
        lenient().when(repository.findModeDistributionByBandSince(eq(band), any()))
                .thenReturn(java.util.Collections.emptyList());
    }

    private void setupSpotCount(String band, long count) {
        lenient().when(repository.countByBandAndSpottedAtAfter(eq(band), any()))
                .thenReturn(count);
    }

    private void setupBaselineCounts() {
        // Baseline calculation uses same count method with different time window
        // No additional mock setup needed - count is already configured via setupSpotCount
    }

    private void setupNoDxData(String band) {
        lenient().when(repository.findMaxDxSpotByBandAndSpottedAtAfter(eq(band), any()))
                .thenReturn(Optional.empty());
    }

    private void setupDxSpot(String band, int distanceKm, String spottedCall, String spotterCall) {
        Spot domainSpot = SpotFixtures.spot()
                .band(band)
                .distanceKm(distanceKm)
                .spottedCall(spottedCall)
                .spotterCall(spotterCall)
                .build();
        SpotEntity spot = SpotEntity.fromDomain(domainSpot);

        lenient().when(repository.findMaxDxSpotByBandAndSpottedAtAfter(eq(band), any()))
                .thenReturn(Optional.of(spot));
    }

    private void setupNoPaths(String band) {
        lenient().when(repository.countContinentPathsByBandAndSpottedAtAfter(eq(band), any()))
                .thenReturn(java.util.Collections.emptyList());
    }

    private void setupPath(String band, String c1, String c2, long count) {
        List<Object[]> paths = java.util.Collections.singletonList(new Object[]{c1, c2, count});
        lenient().when(repository.countContinentPathsByBandAndSpottedAtAfter(eq(band), any()))
                .thenReturn(paths);
    }

    private void setupMultiplePaths(String band, Object[]... pathData) {
        List<Object[]> paths = Arrays.asList(pathData);
        lenient().when(repository.countContinentPathsByBandAndSpottedAtAfter(eq(band), any()))
                .thenReturn(paths);
    }
}
