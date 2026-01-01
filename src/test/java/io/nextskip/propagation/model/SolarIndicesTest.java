package io.nextskip.propagation.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.nextskip.test.TestConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for SolarIndices record.
 *
 * <p>Tests business logic methods for solar activity assessment:
 * - getGeomagneticActivity() - K-index to activity level mapping
 * - getSolarFluxLevel() - SFI to flux level mapping
 * - isFavorable() - Overall propagation favorability
 *
 * <p>Tests use invariant-based assertions to verify scoring contracts
 * without coupling to specific algorithm coefficients.
 */
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Required for invariant testing across value ranges
class SolarIndicesTest {

    private static final String TEST_SOURCE = "Test";
    private static final String ACTIVITY_QUIET = "Quiet";

    // ==========================================================================
    // Category 1: getGeomagneticActivity() Tests
    // ==========================================================================

    @Nested
    class GeomagneticActivityTests {

        @Test
        void testGetGeomagneticActivity_KIndexOrderingInvariant() {
            // Invariant: Higher K-index means more severe activity
            // Activity levels should progress: Quiet -> Unsettled -> Active -> Storm -> Severe Storm
            String[] expectedOrder = {
                    ACTIVITY_QUIET, ACTIVITY_QUIET, ACTIVITY_QUIET,  // K-index 0-2
                    "Unsettled", "Unsettled",                        // K-index 3-4
                    "Active", "Active",                              // K-index 5-6
                    "Storm", "Storm",                                // K-index 7-8
                    "Severe Storm"                                   // K-index 9
            };

            for (int k = 0; k <= 9; k++) {
                var indices = new SolarIndices(DEFAULT_SFI, DEFAULT_A_INDEX, k, DEFAULT_SUNSPOT_NUMBER,
                        Instant.now(), TEST_SOURCE);
                assertThat(indices.getGeomagneticActivity())
                        .as("K-index %d should map to correct activity", k)
                        .isEqualTo(expectedOrder[k]);
            }
        }

        @Test
        void testGetGeomagneticActivity_QuietAtLowKIndex() {
            // Quiet range: K-index 0-2
            for (int k = 0; k <= 2; k++) {
                var indices = new SolarIndices(DEFAULT_SFI, DEFAULT_A_INDEX, k, DEFAULT_SUNSPOT_NUMBER,
                        Instant.now(), TEST_SOURCE);
                assertThat(indices.getGeomagneticActivity())
                        .as("K-index %d should be Quiet", k)
                        .isEqualTo(ACTIVITY_QUIET);
            }
        }

        @Test
        void testGetGeomagneticActivity_SevereStormAtMaxKIndex() {
            var indices = new SolarIndices(DEFAULT_SFI, DEFAULT_A_INDEX, 9, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE);
            assertThat(indices.getGeomagneticActivity())
                    .isEqualTo("Severe Storm");
        }
    }

    // ==========================================================================
    // Category 2: getSolarFluxLevel() Tests
    // ==========================================================================

    @Nested
    class SolarFluxLevelTests {

        @Test
        void testGetSolarFluxLevel_SfiOrderingInvariant() {
            // Invariant: Higher SFI means more active solar conditions
            // Test that flux levels increase with SFI
            double[] testValues = {50.0, 85.0, 125.0, 175.0, 250.0};
            String[] expectedLevels = {"Very Low", "Low", "Moderate", "High", "Very High"};

            for (int i = 0; i < testValues.length; i++) {
                var indices = new SolarIndices(testValues[i], DEFAULT_A_INDEX, DEFAULT_K_INDEX,
                        DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
                assertThat(indices.getSolarFluxLevel())
                        .as("SFI %.1f should be %s", testValues[i], expectedLevels[i])
                        .isEqualTo(expectedLevels[i]);
            }
        }

        @Test
        void testGetSolarFluxLevel_BoundaryValues() {
            // Test boundary transitions
            // < 70 = Very Low, 70-99 = Low, 100-149 = Moderate, 150-199 = High, >= 200 = Very High

            // Very Low boundary
            assertThat(new SolarIndices(69.0, DEFAULT_A_INDEX, DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE).getSolarFluxLevel())
                    .as("SFI 69 should be Very Low")
                    .isEqualTo("Very Low");

            // Low boundary
            assertThat(new SolarIndices(70.0, DEFAULT_A_INDEX, DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE).getSolarFluxLevel())
                    .as("SFI 70 should be Low")
                    .isEqualTo("Low");

            // Moderate boundary
            assertThat(new SolarIndices(100.0, DEFAULT_A_INDEX, DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE).getSolarFluxLevel())
                    .as("SFI 100 should be Moderate")
                    .isEqualTo("Moderate");

            // High boundary
            assertThat(new SolarIndices(150.0, DEFAULT_A_INDEX, DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE).getSolarFluxLevel())
                    .as("SFI 150 should be High")
                    .isEqualTo("High");

            // Very High boundary
            assertThat(new SolarIndices(200.0, DEFAULT_A_INDEX, DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE).getSolarFluxLevel())
                    .as("SFI 200 should be Very High")
                    .isEqualTo("Very High");
        }
    }

    // ==========================================================================
    // Category 3: isFavorable() Invariant Tests
    // ==========================================================================

    @Nested
    class IsFavorableInvariantTests {

        @Test
        void testIsFavorable_RequiresAllConditionsMet() {
            // Invariant: isFavorable requires ALL conditions: SFI > threshold, K < threshold, A < threshold
            // When all conditions pass, should be favorable
            var favorable = new SolarIndices(150.0, 10, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(favorable.isFavorable())
                    .as("All conditions met should be favorable")
                    .isTrue();

            // When ANY condition fails, should NOT be favorable
            var lowSfi = new SolarIndices(SFI_FAVORABLE_THRESHOLD, 10, 2, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE);
            assertThat(lowSfi.isFavorable())
                    .as("SFI at threshold is NOT > threshold")
                    .isFalse();

            var highKIndex = new SolarIndices(150.0, 10, K_INDEX_FAVORABLE_THRESHOLD, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE);
            assertThat(highKIndex.isFavorable())
                    .as("K-index at threshold is NOT < threshold")
                    .isFalse();

            var highAIndex = new SolarIndices(150.0, A_INDEX_FAVORABLE_THRESHOLD, 2, DEFAULT_SUNSPOT_NUMBER,
                    Instant.now(), TEST_SOURCE);
            assertThat(highAIndex.isFavorable())
                    .as("A-index at threshold is NOT < threshold")
                    .isFalse();
        }

        @Test
        void testIsFavorable_SfiBoundaryCondition() {
            // SFI boundary: must be > threshold (strict inequality)
            double threshold = SFI_FAVORABLE_THRESHOLD;

            var atBoundary = new SolarIndices(threshold, 10, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(atBoundary.isFavorable())
                    .as("SFI at threshold should NOT be favorable")
                    .isFalse();

            var justAbove = new SolarIndices(threshold + 0.1, 10, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(justAbove.isFavorable())
                    .as("SFI just above threshold should be favorable")
                    .isTrue();

            var justBelow = new SolarIndices(threshold - 0.1, 10, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(justBelow.isFavorable())
                    .as("SFI just below threshold should NOT be favorable")
                    .isFalse();
        }

        @Test
        void testIsFavorable_KIndexBoundaryCondition() {
            // K-index boundary: must be < threshold (strict inequality)
            int threshold = K_INDEX_FAVORABLE_THRESHOLD;

            var atBoundary = new SolarIndices(150.0, 10, threshold, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(atBoundary.isFavorable())
                    .as("K-index at threshold should NOT be favorable")
                    .isFalse();

            var justBelow = new SolarIndices(150.0, 10, threshold - 1, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(justBelow.isFavorable())
                    .as("K-index just below threshold should be favorable")
                    .isTrue();

            var justAbove = new SolarIndices(150.0, 10, threshold + 1, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(justAbove.isFavorable())
                    .as("K-index just above threshold should NOT be favorable")
                    .isFalse();
        }

        @Test
        void testIsFavorable_AIndexBoundaryCondition() {
            // A-index boundary: must be < threshold (strict inequality)
            int threshold = A_INDEX_FAVORABLE_THRESHOLD;

            var atBoundary = new SolarIndices(150.0, threshold, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(atBoundary.isFavorable())
                    .as("A-index at threshold should NOT be favorable")
                    .isFalse();

            var justBelow = new SolarIndices(150.0, threshold - 1, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(justBelow.isFavorable())
                    .as("A-index just below threshold should be favorable")
                    .isTrue();

            var justAbove = new SolarIndices(150.0, threshold + 1, 2, DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
            assertThat(justAbove.isFavorable())
                    .as("A-index just above threshold should NOT be favorable")
                    .isFalse();
        }
    }

    // ==========================================================================
    // Category 4: getScore() Invariant Tests
    // ==========================================================================

    @Nested
    class ScoreInvariantTests {

        @Test
        void testGetScore_AlwaysWithinBounds() {
            // Score should always be in [0, 100] for any input combination
            double[] sfiValues = {50.0, 100.0, 150.0, 200.0, 300.0};
            int[] kValues = {0, 2, 4, 6, 9};
            int[] aValues = {0, 10, 20, 30, 50};

            for (double sfi : sfiValues) {
                for (int k : kValues) {
                    for (int a : aValues) {
                        var indices = new SolarIndices(sfi, a, k, DEFAULT_SUNSPOT_NUMBER,
                                Instant.now(), TEST_SOURCE);
                        int score = indices.getScore();

                        assertThat(score)
                                .as("SFI=%.1f, K=%d, A=%d should score in [%d, %d]", sfi, k, a, MIN_SCORE, MAX_SCORE)
                                .isBetween(MIN_SCORE, MAX_SCORE);
                    }
                }
            }
        }

        @Test
        void testGetScore_HigherSfiMeansHigherOrEqualScore() {
            // Invariant: Higher SFI should produce higher or equal score (all else equal)
            int[] testSfiValues = {50, 100, 150, 200, 250};
            int previousScore = -1;

            for (int sfi : testSfiValues) {
                var indices = new SolarIndices((double) sfi, DEFAULT_A_INDEX, DEFAULT_K_INDEX,
                        DEFAULT_SUNSPOT_NUMBER, Instant.now(), TEST_SOURCE);
                int score = indices.getScore();

                if (previousScore >= 0) {
                    assertThat(score)
                            .as("SFI %d should score >= previous", sfi)
                            .isGreaterThanOrEqualTo(previousScore);
                }
                previousScore = score;
            }
        }

        @Test
        void testGetScore_LowerKIndexMeansHigherOrEqualScore() {
            // Invariant: Lower K-index should produce higher or equal score (better conditions)
            int previousScore = -1;

            // Test from high K to low K (worst to best)
            for (int k = 9; k >= 0; k--) {
                var indices = new SolarIndices(DEFAULT_SFI, DEFAULT_A_INDEX, k, DEFAULT_SUNSPOT_NUMBER,
                        Instant.now(), TEST_SOURCE);
                int score = indices.getScore();

                if (previousScore >= 0) {
                    assertThat(score)
                            .as("K-index %d should score >= K-index %d", k, k + 1)
                            .isGreaterThanOrEqualTo(previousScore);
                }
                previousScore = score;
            }
        }
    }
}
