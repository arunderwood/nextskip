package io.nextskip.spots.model;

import io.nextskip.common.api.Scoreable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static io.nextskip.test.TestConstants.MAX_SCORE;
import static io.nextskip.test.TestConstants.MIN_SCORE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BandActivity} record.
 *
 * <p>Tests cover the Scoreable interface implementation including scoring calculation,
 * isFavorable conditions, and various helper methods.
 *
 * <p>Scoring is weighted across multiple factors:
 * <ul>
 *   <li>40% - Activity level (spot count)</li>
 *   <li>30% - Trend (positive momentum)</li>
 *   <li>20% - DX reach (distance)</li>
 *   <li>10% - Path diversity</li>
 * </ul>
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data intentionally repeats band/mode values
class BandActivityTest {

    private static final Instant NOW = Instant.now();
    private static final Instant WINDOW_START = NOW.minusSeconds(900); // 15 min ago

    // =========================================================================
    // Record Construction Tests
    // =========================================================================

    @Nested
    class RecordConstructionTests {

        @Test
        void testBandActivity_FullConstruction_AllFieldsSet() {
            Set<ContinentPath> paths = Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS);

            BandActivity activity = new BandActivity(
                    "20m",
                    "FT8",
                    150,          // spotCount
                    100,          // baselineSpotCount
                    50.0,         // trendPercentage
                    8500,         // maxDxKm
                    "JA1ABC → W6XYZ",
                    paths,
                    WINDOW_START,
                    NOW,
                    NOW
            );

            assertThat(activity.band()).isEqualTo("20m");
            assertThat(activity.mode()).isEqualTo("FT8");
            assertThat(activity.spotCount()).isEqualTo(150);
            assertThat(activity.baselineSpotCount()).isEqualTo(100);
            assertThat(activity.trendPercentage()).isEqualTo(50.0);
            assertThat(activity.maxDxKm()).isEqualTo(8500);
            assertThat(activity.maxDxPath()).isEqualTo("JA1ABC → W6XYZ");
            assertThat(activity.activePaths()).containsExactlyInAnyOrder(ContinentPath.NA_EU, ContinentPath.NA_AS);
            assertThat(activity.windowStart()).isEqualTo(WINDOW_START);
            assertThat(activity.windowEnd()).isEqualTo(NOW);
            assertThat(activity.calculatedAt()).isEqualTo(NOW);
        }

        @Test
        void testBandActivity_NullPaths_BecomesEmptySet() {
            BandActivity activity = createBandActivity(100, 0.0, null, null);

            assertThat(activity.activePaths())
                    .as("Null paths should become empty set")
                    .isEmpty();
        }

        @Test
        void testBandActivity_PathsAreDefensiveCopy() {
            Set<ContinentPath> mutablePaths = java.util.EnumSet.of(ContinentPath.NA_EU);

            BandActivity activity = createBandActivity(100, 0.0, null, mutablePaths);

            // Try to modify original set
            mutablePaths.add(ContinentPath.NA_AS);

            assertThat(activity.activePaths())
                    .as("Paths should be defensively copied")
                    .hasSize(1)
                    .containsExactly(ContinentPath.NA_EU);
        }
    }

    // =========================================================================
    // isFavorable() Tests
    // =========================================================================

    @Nested
    class IsFavorableTests {

        @Test
        void testIsFavorable_HighActivityPositiveTrendActivePaths_ReturnsTrue() {
            BandActivity activity = createBandActivity(
                    100,   // >= 100 threshold
                    10.0,  // > 0
                    null,
                    Set.of(ContinentPath.NA_EU)
            );

            assertThat(activity.isFavorable())
                    .as("Should be favorable with high activity, positive trend, and active paths")
                    .isTrue();
        }

        @Test
        void testIsFavorable_BelowActivityThreshold_ReturnsFalse() {
            BandActivity activity = createBandActivity(
                    99,    // < 100 threshold
                    10.0,
                    null,
                    Set.of(ContinentPath.NA_EU)
            );

            assertThat(activity.isFavorable())
                    .as("Should not be favorable with activity below threshold")
                    .isFalse();
        }

        @Test
        void testIsFavorable_ZeroTrend_ReturnsFalse() {
            BandActivity activity = createBandActivity(
                    100,
                    0.0,   // Not > 0
                    null,
                    Set.of(ContinentPath.NA_EU)
            );

            assertThat(activity.isFavorable())
                    .as("Should not be favorable with zero trend")
                    .isFalse();
        }

        @Test
        void testIsFavorable_NegativeTrend_ReturnsFalse() {
            BandActivity activity = createBandActivity(
                    100,
                    -10.0,
                    null,
                    Set.of(ContinentPath.NA_EU)
            );

            assertThat(activity.isFavorable())
                    .as("Should not be favorable with negative trend")
                    .isFalse();
        }

        @Test
        void testIsFavorable_EmptyPaths_ReturnsFalse() {
            BandActivity activity = createBandActivity(
                    100,
                    10.0,
                    null,
                    Set.of()  // Empty paths
            );

            assertThat(activity.isFavorable())
                    .as("Should not be favorable with no active paths")
                    .isFalse();
        }

        @Test
        void testIsFavorable_NullPaths_ReturnsFalse() {
            BandActivity activity = createBandActivity(
                    100,
                    10.0,
                    null,
                    null  // Null paths
            );

            assertThat(activity.isFavorable())
                    .as("Should not be favorable when paths is null (becomes empty)")
                    .isFalse();
        }

        @Test
        void testIsFavorable_ExactlyAtActivityThreshold_ReturnsTrue() {
            BandActivity activity = createBandActivity(
                    100,   // Exactly at threshold
                    0.1,   // Minimally positive
                    null,
                    Set.of(ContinentPath.NA_EU)
            );

            assertThat(activity.isFavorable())
                    .as("Should be favorable at exact activity threshold")
                    .isTrue();
        }
    }

    // =========================================================================
    // getScore() Invariant Tests
    // =========================================================================

    @Nested
    class ScoreInvariantTests {

        @Test
        void testGetScore_AlwaysWithinBounds() {
            // Test various combinations using helper to avoid deeply nested loops
            int[] spotCounts = {0, 10, 50, 100, 500};
            double[] trends = {-100, -50, 0, 20, 50, 100};
            Integer[] dxDistances = {null, 0, 1000, 5000, 10000, 15000};
            List<Set<ContinentPath>> pathSets = List.of(
                    Set.of(),
                    Set.of(ContinentPath.NA_EU),
                    Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS),
                    Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS, ContinentPath.EU_AS, ContinentPath.NA_OC)
            );

            for (int spotCount : spotCounts) {
                for (double trend : trends) {
                    verifyScoreWithinBoundsForAllCombinations(spotCount, trend, dxDistances, pathSets);
                }
            }
        }

        private void verifyScoreWithinBoundsForAllCombinations(
                int spotCount, double trend, Integer[] dxDistances, List<Set<ContinentPath>> pathSets) {
            for (Integer dx : dxDistances) {
                for (Set<ContinentPath> paths : pathSets) {
                    BandActivity activity = createBandActivity(spotCount, trend, dx, paths);
                    int score = activity.getScore();

                    assertThat(score)
                            .as("Score should be within [%d, %d] for spotCount=%d, trend=%.1f, dx=%s, paths=%d",
                                    MIN_SCORE, MAX_SCORE, spotCount, trend, dx, paths.size())
                            .isBetween(MIN_SCORE, MAX_SCORE);
                }
            }
        }

        @Test
        void testGetScore_MoreSpotsScoresHigherOrEqual() {
            // Invariant: more spots should score >= fewer spots (all else equal)
            Set<ContinentPath> paths = Set.of(ContinentPath.NA_EU);

            BandActivity lowActivity = createBandActivity(10, 0.0, 5000, paths);
            BandActivity medActivity = createBandActivity(50, 0.0, 5000, paths);
            BandActivity highActivity = createBandActivity(100, 0.0, 5000, paths);

            assertThat(highActivity.getScore())
                    .as("High activity >= Medium activity")
                    .isGreaterThanOrEqualTo(medActivity.getScore());

            assertThat(medActivity.getScore())
                    .as("Medium activity >= Low activity")
                    .isGreaterThanOrEqualTo(lowActivity.getScore());
        }

        @Test
        void testGetScore_PositiveTrendScoresHigherThanNegative() {
            // Invariant: positive trend should score higher than negative (all else equal)
            Set<ContinentPath> paths = Set.of(ContinentPath.NA_EU);

            BandActivity positive = createBandActivity(50, 30.0, 5000, paths);
            BandActivity negative = createBandActivity(50, -30.0, 5000, paths);

            assertThat(positive.getScore())
                    .as("Positive trend should score higher than negative")
                    .isGreaterThan(negative.getScore());
        }

        @Test
        void testGetScore_LongerDxScoresHigherOrEqual() {
            // Invariant: longer DX should score >= shorter DX (all else equal)
            Set<ContinentPath> paths = Set.of(ContinentPath.NA_EU);

            BandActivity shortDx = createBandActivity(50, 0.0, 1000, paths);
            BandActivity medDx = createBandActivity(50, 0.0, 5000, paths);
            BandActivity longDx = createBandActivity(50, 0.0, 10000, paths);

            assertThat(longDx.getScore())
                    .as("Long DX >= Medium DX")
                    .isGreaterThanOrEqualTo(medDx.getScore());

            assertThat(medDx.getScore())
                    .as("Medium DX >= Short DX")
                    .isGreaterThanOrEqualTo(shortDx.getScore());
        }

        @Test
        void testGetScore_MorePathsScoresHigherOrEqual() {
            // Invariant: more active paths should score >= fewer paths (all else equal)
            BandActivity noPaths = createBandActivity(50, 0.0, 5000, Set.of());
            BandActivity onePath = createBandActivity(50, 0.0, 5000, Set.of(ContinentPath.NA_EU));
            BandActivity twoPaths = createBandActivity(50, 0.0, 5000,
                    Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS));
            BandActivity fourPaths = createBandActivity(50, 0.0, 5000,
                    Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS, ContinentPath.EU_AS, ContinentPath.NA_OC));

            assertThat(fourPaths.getScore())
                    .as("4 paths >= 2 paths")
                    .isGreaterThanOrEqualTo(twoPaths.getScore());

            assertThat(twoPaths.getScore())
                    .as("2 paths >= 1 path")
                    .isGreaterThanOrEqualTo(onePath.getScore());

            assertThat(onePath.getScore())
                    .as("1 path >= 0 paths")
                    .isGreaterThanOrEqualTo(noPaths.getScore());
        }
    }

    // =========================================================================
    // getScore() Boundary Tests
    // =========================================================================

    @Nested
    class ScoreBoundaryTests {

        @Test
        void testGetScore_OptimalConditions_ApproachesMaxScore() {
            // Optimal: high activity, strong positive trend, excellent DX, many paths
            BandActivity optimal = createBandActivity(
                    150,     // High activity
                    60.0,    // Strong positive trend
                    12000,   // Excellent DX
                    Set.of(ContinentPath.NA_EU, ContinentPath.NA_AS, ContinentPath.EU_AS, ContinentPath.NA_OC)
            );

            assertThat(optimal.getScore())
                    .as("Optimal conditions should approach max score")
                    .isGreaterThanOrEqualTo(90);
        }

        @Test
        void testGetScore_WorstConditions_ApproachesMinScore() {
            // Worst: no activity, strong negative trend, no DX, no paths
            BandActivity worst = createBandActivity(
                    0,       // No activity
                    -100.0,  // Strong negative trend
                    null,    // No DX
                    Set.of() // No paths
            );

            assertThat(worst.getScore())
                    .as("Worst conditions should approach min score")
                    .isLessThanOrEqualTo(10);
        }

        @Test
        void testGetScore_NullMaxDx_DoesNotThrow() {
            BandActivity activity = createBandActivity(50, 0.0, null, Set.of(ContinentPath.NA_EU));

            assertThat(activity.getScore())
                    .as("Null maxDxKm should not throw")
                    .isBetween(MIN_SCORE, MAX_SCORE);
        }

        @Test
        void testGetScore_ZeroMaxDx_TreatedAsNoDxData() {
            BandActivity zeroDx = createBandActivity(50, 0.0, 0, Set.of(ContinentPath.NA_EU));
            BandActivity nullDx = createBandActivity(50, 0.0, null, Set.of(ContinentPath.NA_EU));

            assertThat(zeroDx.getScore())
                    .as("Zero DX should score same as null DX")
                    .isEqualTo(nullDx.getScore());
        }
    }

    // =========================================================================
    // Activity Level Scoring Tests
    // =========================================================================

    @Nested
    class ActivityLevelScoringTests {

        @Test
        void testActivityScore_HighActivity_ScoresHigh() {
            // Activity component is 40% of total score
            // 100+ spots = 100 points for activity component
            BandActivity high = createBandActivityWithOnlyActivity(150);
            BandActivity medium = createBandActivityWithOnlyActivity(50);
            BandActivity low = createBandActivityWithOnlyActivity(10);

            assertThat(high.getScore())
                    .isGreaterThan(medium.getScore());
            assertThat(medium.getScore())
                    .isGreaterThan(low.getScore());
        }

        @Test
        void testActivityScore_LinearInterpolation_InTiers() {
            // Test that scores increase monotonically through each tier
            int previousScore = 0;
            for (int spots = 0; spots <= 100; spots += 10) {
                BandActivity activity = createBandActivityWithOnlyActivity(spots);
                int score = activity.getScore();

                assertThat(score)
                        .as("Score at %d spots should be >= score at previous tier", spots)
                        .isGreaterThanOrEqualTo(previousScore);
                previousScore = score;
            }
        }
    }

    // =========================================================================
    // Trend Scoring Tests
    // =========================================================================

    @Nested
    class TrendScoringTests {

        @Test
        void testTrendScore_StrongPositive_ScoresHigh() {
            BandActivity strongPositive = createBandActivityWithOnlyTrend(60.0);
            BandActivity moderate = createBandActivityWithOnlyTrend(20.0);
            BandActivity flat = createBandActivityWithOnlyTrend(0.0);

            assertThat(strongPositive.getScore())
                    .isGreaterThan(moderate.getScore());
            assertThat(moderate.getScore())
                    .isGreaterThan(flat.getScore());
        }

        @Test
        void testTrendScore_Negative_ScoresLowerThanFlat() {
            BandActivity flat = createBandActivityWithOnlyTrend(0.0);
            BandActivity negative = createBandActivityWithOnlyTrend(-50.0);

            assertThat(flat.getScore())
                    .as("Flat trend should score higher than negative")
                    .isGreaterThan(negative.getScore());
        }
    }

    // =========================================================================
    // DX Distance Scoring Tests
    // =========================================================================

    @Nested
    class DxDistanceScoringTests {

        @Test
        void testDxScore_ExcellentDx_ScoresHigh() {
            BandActivity excellent = createBandActivityWithOnlyDx(12000);
            BandActivity good = createBandActivityWithOnlyDx(7000);
            BandActivity moderate = createBandActivityWithOnlyDx(3000);
            BandActivity shortDx = createBandActivityWithOnlyDx(1000);

            assertThat(excellent.getScore()).isGreaterThan(good.getScore());
            assertThat(good.getScore()).isGreaterThan(moderate.getScore());
            assertThat(moderate.getScore()).isGreaterThan(shortDx.getScore());
        }

        @Test
        void testDxScore_NullDx_ScoresLow() {
            BandActivity nullDx = createBandActivityWithOnlyDx(null);
            BandActivity someDx = createBandActivityWithOnlyDx(5000);

            assertThat(someDx.getScore())
                    .as("Some DX should score higher than null DX")
                    .isGreaterThan(nullDx.getScore());
        }
    }

    // =========================================================================
    // Helper Methods Tests
    // =========================================================================

    @Nested
    class HelperMethodTests {

        @Test
        void testGetWindowMinutes_CalculatesCorrectly() {
            Instant start = NOW.minusSeconds(900); // 15 minutes ago
            BandActivity activity = new BandActivity(
                    "20m", "FT8", 100, 80, 25.0, 5000, "W1AW → G3ABC",
                    Set.of(ContinentPath.NA_EU), start, NOW, NOW
            );

            assertThat(activity.getWindowMinutes())
                    .as("Window should be 15 minutes")
                    .isEqualTo(15);
        }

        @Test
        void testGetWindowMinutes_NullWindowStart_ReturnsZero() {
            BandActivity activity = new BandActivity(
                    "20m", "FT8", 100, 80, 25.0, 5000, null,
                    Set.of(), null, NOW, NOW
            );

            assertThat(activity.getWindowMinutes())
                    .as("Null window start should return 0")
                    .isZero();
        }

        @Test
        void testGetWindowMinutes_NullWindowEnd_ReturnsZero() {
            BandActivity activity = new BandActivity(
                    "20m", "FT8", 100, 80, 25.0, 5000, null,
                    Set.of(), NOW, null, NOW
            );

            assertThat(activity.getWindowMinutes())
                    .as("Null window end should return 0")
                    .isZero();
        }

        @Test
        void testHasActivity_WithSpots_ReturnsTrue() {
            BandActivity activity = createBandActivity(10, 0.0, null, Set.of());

            assertThat(activity.hasActivity())
                    .as("Should have activity when spotCount > 0")
                    .isTrue();
        }

        @Test
        void testHasActivity_ZeroSpots_ReturnsFalse() {
            BandActivity activity = createBandActivity(0, 0.0, null, Set.of());

            assertThat(activity.hasActivity())
                    .as("Should not have activity when spotCount = 0")
                    .isFalse();
        }

        @Test
        void testHasDxData_WithPositiveDx_ReturnsTrue() {
            BandActivity activity = createBandActivity(10, 0.0, 5000, Set.of());

            assertThat(activity.hasDxData())
                    .as("Should have DX data when maxDxKm > 0")
                    .isTrue();
        }

        @Test
        void testHasDxData_NullDx_ReturnsFalse() {
            BandActivity activity = createBandActivity(10, 0.0, null, Set.of());

            assertThat(activity.hasDxData())
                    .as("Should not have DX data when maxDxKm is null")
                    .isFalse();
        }

        @Test
        void testHasDxData_ZeroDx_ReturnsFalse() {
            BandActivity activity = createBandActivity(10, 0.0, 0, Set.of());

            assertThat(activity.hasDxData())
                    .as("Should not have DX data when maxDxKm = 0")
                    .isFalse();
        }
    }

    // =========================================================================
    // Interface Compliance Tests
    // =========================================================================

    @Nested
    class InterfaceComplianceTests {

        @Test
        void testBandActivity_ImplementsScoreable() {
            BandActivity activity = createBandActivity(100, 0.0, 5000, Set.of(ContinentPath.NA_EU));

            assertThat(activity)
                    .as("BandActivity should implement Scoreable interface")
                    .isInstanceOf(Scoreable.class);
        }

        @Test
        void testScoreable_GetScoreReturnsInt() {
            Scoreable scoreable = createBandActivity(100, 0.0, 5000, Set.of());
            int score = scoreable.getScore();

            assertThat(score)
                    .as("Scoreable.getScore() should return valid int")
                    .isBetween(MIN_SCORE, MAX_SCORE);
        }

        @Test
        void testScoreable_IsFavorableReturnsBoolean() {
            Scoreable scoreable = createBandActivity(100, 10.0, 5000, Set.of(ContinentPath.NA_EU));
            boolean favorable = scoreable.isFavorable();

            assertThat(favorable)
                    .as("Scoreable.isFavorable() should return boolean")
                    .isTrue();
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private BandActivity createBandActivity(int spotCount, double trend, Integer maxDxKm,
                                            Set<ContinentPath> paths) {
        return new BandActivity(
                "20m",
                "FT8",
                spotCount,
                50,              // baseline
                trend,
                maxDxKm,
                maxDxKm != null ? "W1AW → G3ABC" : null,
                paths,
                WINDOW_START,
                NOW,
                NOW
        );
    }

    /**
     * Create BandActivity with only activity varying (for testing activity scoring).
     */
    private BandActivity createBandActivityWithOnlyActivity(int spotCount) {
        return new BandActivity(
                "20m", "FT8", spotCount, 50, 0.0, null, null,
                Set.of(), WINDOW_START, NOW, NOW
        );
    }

    /**
     * Create BandActivity with only trend varying (for testing trend scoring).
     */
    private BandActivity createBandActivityWithOnlyTrend(double trend) {
        return new BandActivity(
                "20m", "FT8", 50, 50, trend, null, null,
                Set.of(), WINDOW_START, NOW, NOW
        );
    }

    /**
     * Create BandActivity with only DX varying (for testing DX scoring).
     */
    private BandActivity createBandActivityWithOnlyDx(Integer maxDxKm) {
        return new BandActivity(
                "20m", "FT8", 50, 50, 0.0, maxDxKm,
                maxDxKm != null && maxDxKm > 0 ? "W1AW → G3ABC" : null,
                Set.of(), WINDOW_START, NOW, NOW
        );
    }
}
