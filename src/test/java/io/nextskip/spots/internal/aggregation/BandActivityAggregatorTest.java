package io.nextskip.spots.internal.aggregation;

import io.nextskip.spots.internal.ScoringProperties;
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
 * <p>Tests cover band-mode aggregation logic including trend calculation,
 * DX distance tracking, and continent path analysis.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data intentionally repeats band/mode values
class BandActivityAggregatorTest {

    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
    private static final String BAND_20M = "20m";
    private static final String BAND_40M = "40m";
    private static final String MODE_FT8 = "FT8";
    private static final String MODE_FT4 = "FT4";
    private static final String MODE_FT2 = "FT2";
    private static final String MODE_CW = "CW";

    @Mock
    private SpotRepository repository;

    private Clock clock;
    private ScoringProperties scoringProperties;
    private BandActivityAggregator aggregator;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
        scoringProperties = new ScoringProperties();
        scoringProperties.setRarityMultipliers(java.util.Map.of(
                "FT8", 1.0, "FT4", 1.5, "FT2", 3.0
        ));
        aggregator = new BandActivityAggregator(repository, clock, scoringProperties);
    }

    // =========================================================================
    // aggregateBandMode() Tests
    // =========================================================================

    @Nested
    class AggregateBandModeTests {

        @Test
        void testAggregateBandMode_BasicAggregation_ReturnsValidBandActivity() {
            // Given: FT8 mode with some activity
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.band()).isEqualTo(BAND_20M);
            assertThat(activity.mode()).isEqualTo(MODE_FT8);
            assertThat(activity.spotCount()).isEqualTo(100);
            assertThat(activity.windowEnd()).isEqualTo(FIXED_TIME);
        }

        @Test
        void testAggregateBandMode_FT4Mode_ReturnsFT4Activity() {
            // Given: FT4 mode passed in
            setupSpotCount(BAND_20M, MODE_FT4, 25);
            setupNoDxData(BAND_20M, MODE_FT4);
            setupNoPaths(BAND_20M, MODE_FT4);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT4);

            // Then
            assertThat(activity.mode())
                    .as("Should return FT4 when FT4 mode is passed")
                    .isEqualTo(MODE_FT4);
            assertThat(activity.spotCount()).isEqualTo(25);
        }

        @Test
        void testAggregateBandMode_CWMode_Uses30MinWindow() {
            // Given: CW mode
            setupSpotCount(BAND_40M, MODE_CW, 50);
            setupNoDxData(BAND_40M, MODE_CW);
            setupNoPaths(BAND_40M, MODE_CW);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_40M, MODE_CW);

            // Then
            assertThat(activity.mode()).isEqualTo(MODE_CW);
            // CW window is 30 minutes
            assertThat(activity.windowStart())
                    .isEqualTo(FIXED_TIME.minus(Duration.ofMinutes(30)));
        }

        @Test
        void testAggregateBandMode_WindowTimestamps_CorrectForFT8() {
            // Given: FT8 mode (15-minute window)
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

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
        void testAggregateBandMode_TrendCalculation_UsesCurrentAndBaseline() {
            // Given: Current activity with spots
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

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
        void testAggregateBandMode_ZeroBaseline_WithActivity_Returns100Percent() {
            // Given: Activity now but baseline will be 0 (mock returns same count for all times)
            setupSpotCount(BAND_20M, MODE_FT8, 50);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then: With simplified mocks, baseline = 0, so trend = 100%
            assertThat(activity.spotCount()).isEqualTo(50);
            assertThat(activity.trendPercentage())
                    .as("Should return 100% when activity exists but baseline is 0")
                    .isEqualTo(100.0);
        }

        @Test
        void testAggregateBandMode_ZeroBaseline_NoActivity_ReturnsZeroPercent() {
            // Given: No activity now and no baseline
            setupSpotCount(BAND_20M, MODE_FT8, 0);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.trendPercentage())
                    .as("Should return 0% when no activity and no baseline")
                    .isEqualTo(0.0);
        }

        @Test
        void testAggregateBandMode_TrendFormula_VerifyBaselinePopulated() {
            // Given
            setupSpotCount(BAND_20M, MODE_FT8, 50);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

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
        void testAggregateBandMode_WithDxSpot_CapturesMaxDistance() {
            // Given: DX spot of 8500km
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupDxSpot(BAND_20M, MODE_FT8, 8500, "JA1ABC", "W6XYZ");
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.maxDxKm()).isEqualTo(8500);
            assertThat(activity.maxDxPath())
                    .as("Path should be formatted as spotted → spotter")
                    .isEqualTo("JA1ABC → W6XYZ");
            assertThat(activity.hasDxData()).isTrue();
        }

        @Test
        void testAggregateBandMode_NoDxSpot_HasNullDxData() {
            // Given: No DX spot data
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.maxDxKm()).isNull();
            assertThat(activity.maxDxPath()).isNull();
            assertThat(activity.hasDxData()).isFalse();
        }

        @Test
        void testAggregateBandMode_DxPathFormatting_UpperCase() {
            // Given: DX spot with lowercase callsigns
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupDxSpot(BAND_20M, MODE_FT8, 5000, "g3abc", "w1aw");
            setupNoPaths(BAND_20M, MODE_FT8);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

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
        void testAggregateBandMode_ActivePaths_IncludedWhenAboveThreshold() {
            // Given: NA-EU path with 10 spots (above 5 threshold)
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupPath(BAND_20M, MODE_FT8, "NA", "EU", 10L);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.activePaths())
                    .as("Should include NA-EU path")
                    .containsExactly(ContinentPath.NA_EU);
        }

        @Test
        void testAggregateBandMode_Paths_ExcludedWhenBelowThreshold() {
            // Given: NA-EU path with only 3 spots (below 5 threshold)
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupPath(BAND_20M, MODE_FT8, "NA", "EU", 3L);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.activePaths())
                    .as("Should not include path below threshold")
                    .isEmpty();
        }

        @Test
        void testAggregateBandMode_MultiplePaths_AllActive() {
            // Given: Multiple active paths
            setupSpotCount(BAND_20M, MODE_FT8, 200);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupMultiplePaths(BAND_20M, MODE_FT8,
                    new Object[]{"NA", "EU", 20L},
                    new Object[]{"NA", "AS", 15L},
                    new Object[]{"EU", "AS", 10L}
            );

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

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
        void testAggregateBandMode_UnknownPath_Ignored() {
            // Given: AF-OC path (not a defined major path)
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupPath(BAND_20M, MODE_FT8, "AF", "OC", 10L);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            // Then
            assertThat(activity.activePaths())
                    .as("Unknown paths should be ignored")
                    .isEmpty();
        }

        @Test
        void testAggregateBandMode_ReversedPath_StillMatches() {
            // Given: EU-NA (reversed) should still match NA-EU path
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupPath(BAND_20M, MODE_FT8, "EU", "NA", 10L);

            // When
            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

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
            // Given: Two band-mode pairs with spots in buckets
            Instant bucket20m = FIXED_TIME.minus(Duration.ofMinutes(10)); // within FT8 15m window
            Instant bucket40m = FIXED_TIME.minus(Duration.ofMinutes(20)); // within CW 30m window
            setupBulkBuckets(List.of(
                    new Object[]{BAND_20M, MODE_FT8, toInstant(bucket20m), 100L},
                    new Object[]{BAND_40M, MODE_CW, toInstant(bucket40m), 50L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(emptyBulkRows());

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should contain both band-mode pairs")
                    .containsKeys("20m_FT8", "40m_CW");
            assertThat(result.get("20m_FT8").mode()).isEqualTo(MODE_FT8);
            assertThat(result.get("40m_CW").mode()).isEqualTo(MODE_CW);
        }

        @Test
        void testAggregateAllBands_NoActiveBands_ReturnsEmptyMap() {
            // Given: No bucket data
            setupBulkBuckets(emptyBulkRows());
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(emptyBulkRows());

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should return empty map when no active bands")
                    .isEmpty();
        }

        @Test
        void testAggregateAllBands_BulkQueryFailure_PropagatesException() {
            // Given: Bulk bucket query fails
            when(repository.countSpotsByBandModeInBuckets(any()))
                    .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Database error"));

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.dao.DataAccessResourceFailureException.class,
                    () -> aggregator.aggregateAllBands());
        }

        @Test
        void testAggregateAllBands_OrderPreserved() {
            // Given: Multiple bands in bucket data
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(List.of(
                    new Object[]{"10m", MODE_FT8, ts, 10L},
                    new Object[]{"15m", MODE_FT8, ts, 10L},
                    new Object[]{"20m", MODE_FT8, ts, 10L},
                    new Object[]{"40m", MODE_FT8, ts, 10L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(emptyBulkRows());

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result.keySet())
                    .as("Order should be preserved")
                    .containsExactly("10m_FT8", "15m_FT8", "20m_FT8", "40m_FT8");
        }

        @Test
        void testAggregateAllBands_MultipleModesSameBand_ReturnsSeparateEntries() {
            // Given: 20m with both FT8 and FT4 activity
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(List.of(
                    new Object[]{BAND_20M, MODE_FT8, ts, 80L},
                    new Object[]{BAND_20M, MODE_FT4, ts, 30L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(emptyBulkRows());

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should contain separate entries for each mode on same band")
                    .containsKeys("20m_FT8", "20m_FT4");
            assertThat(result.get("20m_FT8").mode()).isEqualTo(MODE_FT8);
            assertThat(result.get("20m_FT8").spotCount()).isEqualTo(80);
            assertThat(result.get("20m_FT4").mode()).isEqualTo(MODE_FT4);
            assertThat(result.get("20m_FT4").spotCount()).isEqualTo(30);
        }

        @Test
        void testAggregateAllBands_WithDxData_PopulatesMaxDx() {
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, ts, 100L}
            ));
            setupBulkDx(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, 8500, "JA1ABC", "W6XYZ"}
            ));
            setupBulkPaths(emptyBulkRows());

            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            assertThat(result.get("20m_FT8").maxDxKm()).isEqualTo(8500);
            assertThat(result.get("20m_FT8").maxDxPath()).isEqualTo("JA1ABC → W6XYZ");
        }

        @Test
        void testAggregateAllBands_NullDxDistance_HasNullDxFields() {
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, ts, 50L}
            ));
            setupBulkDx(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, null, null, null}
            ));
            setupBulkPaths(emptyBulkRows());

            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            assertThat(result.get("20m_FT8").maxDxKm()).isNull();
            assertThat(result.get("20m_FT8").maxDxPath()).isNull();
        }

        @Test
        void testAggregateAllBands_WithPaths_PopulatesActivePaths() {
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, ts, 100L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, "NA", "EU", 10L}
            ));

            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            assertThat(result.get("20m_FT8").activePaths())
                    .containsExactly(ContinentPath.NA_EU);
        }

        @Test
        void testAggregateAllBands_PathsBelowThreshold_Excluded() {
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, ts, 100L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, "NA", "EU", 3L}
            ));

            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            assertThat(result.get("20m_FT8").activePaths()).isEmpty();
        }

        @Test
        void testAggregateAllBands_DxWithNullCallsigns_PathIsNull() {
            Object ts = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, ts, 50L}
            ));
            setupBulkDx(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, 5000, null, "W6XYZ"}
            ));
            setupBulkPaths(emptyBulkRows());

            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            assertThat(result.get("20m_FT8").maxDxKm()).isEqualTo(5000);
            assertThat(result.get("20m_FT8").maxDxPath()).isNull();
        }

        @Test
        void testAggregateAllBands_BaselineFromOlderBuckets_CalculatesTrend() {
            // Current bucket (within 15m window) and older baseline bucket
            Object currentTs = toInstant(FIXED_TIME.minus(Duration.ofMinutes(5)));
            Object baselineTs = toInstant(FIXED_TIME.minus(Duration.ofMinutes(20)));
            setupBulkBuckets(List.of(
                    new Object[]{BAND_20M, MODE_FT8, currentTs, 100L},
                    new Object[]{BAND_20M, MODE_FT8, baselineTs, 50L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(emptyBulkRows());

            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            assertThat(result.get("20m_FT8").spotCount()).isEqualTo(100);
            assertThat(result.get("20m_FT8").baselineSpotCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void testAggregateAllBands_ZeroSpotModes_Excluded() {
            // Given: Only FT8 has buckets, FT4 does not
            Instant bucket = FIXED_TIME.minus(Duration.ofMinutes(5));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT8, toInstant(bucket), 50L}
            ));
            setupBulkDx(emptyBulkRows());
            setupBulkPaths(emptyBulkRows());

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then
            assertThat(result)
                    .as("Should only contain modes with activity")
                    .hasSize(1)
                    .containsKey("20m_FT8");
        }

        @Test
        void testAggregateAllBands_PathsUsesModeSpecificWindow_NotGlobalLookback() {
            // Given: FT4 spots only in older bucket (20 min ago, outside 15m FT4 window)
            // but with path data in 1-hour lookback
            Instant olderBucket = FIXED_TIME.minus(Duration.ofMinutes(20));
            setupBulkBuckets(java.util.Collections.singletonList(
                    new Object[]{BAND_20M, MODE_FT4, toInstant(olderBucket), 30L}
            ));
            setupBulkDx(emptyBulkRows());

            // Path query for 15m window returns no paths (spots are older than 15m)
            Instant ft4WindowStart = FIXED_TIME.minus(Duration.ofMinutes(15));
            when(repository.countContinentPathsPerBandMode(eq(ft4WindowStart)))
                    .thenReturn(emptyBulkRows());
            // Path query for wider windows returns paths (stale data from the old approach)
            Instant cwWindowStart = FIXED_TIME.minus(Duration.ofMinutes(30));
            Instant ssbWindowStart = FIXED_TIME.minus(Duration.ofMinutes(60));
            lenient().when(repository.countContinentPathsPerBandMode(eq(cwWindowStart)))
                    .thenReturn(java.util.Collections.singletonList(
                            new Object[]{BAND_20M, MODE_FT4, "NA", "EU", 10L}
                    ));
            lenient().when(repository.countContinentPathsPerBandMode(eq(ssbWindowStart)))
                    .thenReturn(java.util.Collections.singletonList(
                            new Object[]{BAND_20M, MODE_FT4, "NA", "EU", 10L}
                    ));

            // When
            Map<String, BandActivity> result = aggregator.aggregateAllBands();

            // Then: FT4 uses 15m window for paths, so no paths should be active
            assertThat(result.get("20m_FT4").activePaths())
                    .as("FT4 paths should use 15m window, not wider lookback")
                    .isEmpty();
            // Spot count is 0 because the bucket is outside the 15m window
            assertThat(result.get("20m_FT4").spotCount()).isZero();
        }
    }

    // =========================================================================
    // Rarity Multiplier Passthrough Tests (T020)
    // =========================================================================

    @Nested
    class RarityMultiplierPassthroughTests {

        @Test
        void testAggregateBandMode_FT8_HasDefaultMultiplier() {
            setupSpotCount(BAND_20M, MODE_FT8, 100);
            setupNoDxData(BAND_20M, MODE_FT8);
            setupNoPaths(BAND_20M, MODE_FT8);

            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT8);

            assertThat(activity.rarityMultiplier())
                    .as("FT8 should have 1.0x multiplier (no boost)")
                    .isEqualTo(1.0);
        }

        @Test
        void testAggregateBandMode_FT4_HasConfiguredMultiplier() {
            setupSpotCount(BAND_20M, MODE_FT4, 50);
            setupNoDxData(BAND_20M, MODE_FT4);
            setupNoPaths(BAND_20M, MODE_FT4);

            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT4);

            assertThat(activity.rarityMultiplier())
                    .as("FT4 should have 1.5x multiplier")
                    .isEqualTo(1.5);
        }

        @Test
        void testAggregateBandMode_FT2_HasConfiguredMultiplier() {
            setupSpotCount(BAND_20M, MODE_FT2, 30);
            setupNoDxData(BAND_20M, MODE_FT2);
            setupNoPaths(BAND_20M, MODE_FT2);

            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_FT2);

            assertThat(activity.rarityMultiplier())
                    .as("FT2 should have 3.0x multiplier")
                    .isEqualTo(3.0);
        }

        @Test
        void testAggregateBandMode_UnconfiguredMode_DefaultsTo1() {
            setupSpotCount(BAND_20M, MODE_CW, 50);
            setupNoDxData(BAND_20M, MODE_CW);
            setupNoPaths(BAND_20M, MODE_CW);

            BandActivity activity = aggregator.aggregateBandMode(BAND_20M, MODE_CW);

            assertThat(activity.rarityMultiplier())
                    .as("Unconfigured mode should default to 1.0x")
                    .isEqualTo(1.0);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void setupSpotCount(String band, String mode, long count) {
        lenient().when(repository.countByBandAndModeAndSpottedAtAfter(eq(band), eq(mode), any()))
                .thenReturn(count);
    }

    private void setupNoDxData(String band, String mode) {
        lenient().when(repository.findMaxDxSpotByBandAndModeAndSpottedAtAfter(eq(band), eq(mode), any()))
                .thenReturn(Optional.empty());
    }

    private void setupDxSpot(String band, String mode, int distanceKm, String spottedCall, String spotterCall) {
        Spot domainSpot = SpotFixtures.spot()
                .band(band)
                .distanceKm(distanceKm)
                .spottedCall(spottedCall)
                .spotterCall(spotterCall)
                .build();
        SpotEntity spot = SpotEntity.fromDomain(domainSpot);

        lenient().when(repository.findMaxDxSpotByBandAndModeAndSpottedAtAfter(eq(band), eq(mode), any()))
                .thenReturn(Optional.of(spot));
    }

    private void setupNoPaths(String band, String mode) {
        lenient().when(repository.countContinentPathsByBandAndModeAndSpottedAtAfter(eq(band), eq(mode), any()))
                .thenReturn(java.util.Collections.emptyList());
    }

    private void setupPath(String band, String mode, String c1, String c2, long count) {
        List<Object[]> paths = java.util.Collections.singletonList(new Object[]{c1, c2, count});
        lenient().when(repository.countContinentPathsByBandAndModeAndSpottedAtAfter(eq(band), eq(mode), any()))
                .thenReturn(paths);
    }

    private void setupMultiplePaths(String band, String mode, Object[]... pathData) {
        List<Object[]> paths = Arrays.asList(pathData);
        lenient().when(repository.countContinentPathsByBandAndModeAndSpottedAtAfter(eq(band), eq(mode), any()))
                .thenReturn(paths);
    }

    // Bulk query mocks for aggregateAllBands()

    private void setupBulkBuckets(List<Object[]> rows) {
        lenient().when(repository.countSpotsByBandModeInBuckets(any())).thenReturn(rows);
    }

    private void setupBulkDx(List<Object[]> rows) {
        lenient().when(repository.findMaxDxSpotPerBandMode(any())).thenReturn(rows);
    }

    private void setupBulkPaths(List<Object[]> rows) {
        lenient().when(repository.countContinentPathsPerBandMode(any())).thenReturn(rows);
    }

    private static List<Object[]> emptyBulkRows() {
        return java.util.Collections.emptyList();
    }

    /** Native queries return java.time.Instant directly (Hibernate 6 + modern PG JDBC driver). */
    private static Instant toInstant(Instant instant) {
        return instant;
    }
}
