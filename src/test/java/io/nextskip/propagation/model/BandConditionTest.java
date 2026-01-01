package io.nextskip.propagation.model;

import io.nextskip.common.model.FrequencyBand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.nextskip.test.TestConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for BandCondition record.
 *
 * <p>Tests the core scoring system used for dashboard card prioritization:
 * - Constructor validation (confidence 0.0-1.0)
 * - isFavorable() logic (GOOD rating && confidence > 0.5)
 * - getScore() calculation (rating × confidence)
 *
 * <p>Tests use invariant-based assertions to verify scoring contracts
 * without coupling to specific algorithm coefficients.
 */
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Required for invariant testing across value ranges
class BandConditionTest {

    // ==========================================================================
    // Category 1: Constructor Validation
    // ==========================================================================

    @Nested
    class ConstructorValidationTests {

        @Test
        void testConstructor_ValidConfidenceBoundaries() {
            // Confidence at boundaries should be valid
            var conditionMin = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.0, null);
            assertThat(conditionMin.confidence()).isEqualTo(0.0);

            var conditionMax = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.0, null);
            assertThat(conditionMax.confidence()).isEqualTo(1.0);
        }

        @Test
        void testConstructor_ConfidenceBelowZero_ThrowsException() {
            assertThatThrownBy(() ->
                    new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, -0.001, null)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confidence must be between 0.0 and 1.0");
        }

        @Test
        void testConstructor_ConfidenceAboveOne_ThrowsException() {
            assertThatThrownBy(() ->
                    new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.001, null)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confidence must be between 0.0 and 1.0");
        }
    }

    // ==========================================================================
    // Category 2: Secondary Constructors - Defaults
    // ==========================================================================

    @Nested
    class SecondaryConstructorTests {

        @Test
        void testTwoArgConstructor_DefaultsConfidenceToOne() {
            var condition = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR);

            assertThat(condition.band()).isEqualTo(FrequencyBand.BAND_40M);
            assertThat(condition.rating()).isEqualTo(BandConditionRating.FAIR);
            assertThat(condition.confidence()).isEqualTo(1.0);
            assertThat(condition.notes()).isNull();
        }

        @Test
        void testThreeArgConstructor_SetsConfidenceNoNotes() {
            var condition = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 0.7);

            assertThat(condition.band()).isEqualTo(FrequencyBand.BAND_80M);
            assertThat(condition.rating()).isEqualTo(BandConditionRating.POOR);
            assertThat(condition.confidence()).isEqualTo(0.7);
            assertThat(condition.notes()).isNull();
        }
    }

    // ==========================================================================
    // Category 3: isFavorable() Tests
    // ==========================================================================

    @Nested
    class IsFavorableTests {

        @Test
        void testIsFavorable_GoodWithHighConfidence_ReturnsTrue() {
            // GOOD rating with confidence > 0.5 should be favorable
            var condition = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.6);
            assertThat(condition.isFavorable()).isTrue();

            var conditionFull = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.0);
            assertThat(conditionFull.isFavorable()).isTrue();
        }

        @Test
        void testIsFavorable_GoodAtExactThreshold_ReturnsFalse() {
            // GOOD rating with confidence = 0.5 should NOT be favorable (strict inequality)
            var condition = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.5);
            assertThat(condition.isFavorable())
                    .as("0.5 is NOT > 0.5, so should not be favorable")
                    .isFalse();
        }

        @Test
        void testIsFavorable_GoodWithLowConfidence_ReturnsFalse() {
            var condition = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.3);
            assertThat(condition.isFavorable()).isFalse();
        }

        @Test
        void testIsFavorable_NonGoodRating_NeverFavorable() {
            // Non-GOOD ratings should never be favorable, regardless of confidence
            for (BandConditionRating rating : new BandConditionRating[]{
                    BandConditionRating.FAIR, BandConditionRating.POOR, BandConditionRating.UNKNOWN}) {
                var condition = new BandCondition(FrequencyBand.BAND_40M, rating, 1.0);
                assertThat(condition.isFavorable())
                        .as("%s rating should not be favorable", rating)
                        .isFalse();
            }
        }
    }

    // ==========================================================================
    // Category 4: getScore() Invariant Tests
    // ==========================================================================

    @Nested
    class ScoreInvariantTests {

        @Test
        void testGetScore_AlwaysWithinBounds() {
            // Score should always be in [0, 100] for any valid rating/confidence
            for (BandConditionRating rating : BandConditionRating.values()) {
                for (double confidence : new double[]{0.0, 0.25, 0.5, 0.75, 1.0}) {
                    var condition = new BandCondition(FrequencyBand.BAND_20M, rating, confidence);
                    int score = condition.getScore();

                    assertThat(score)
                            .as("Score for %s at %.2f confidence", rating, confidence)
                            .isBetween(MIN_SCORE, MAX_SCORE);
                }
            }
        }

        @Test
        void testGetScore_RatingOrderingInvariant() {
            // Invariant: GOOD >= FAIR >= POOR >= UNKNOWN for same confidence
            double confidence = 1.0;

            int goodScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, confidence).getScore();
            int fairScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.FAIR, confidence).getScore();
            int poorScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.POOR, confidence).getScore();
            int unknownScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.UNKNOWN, confidence).getScore();

            assertThat(goodScore).as("GOOD >= FAIR").isGreaterThanOrEqualTo(fairScore);
            assertThat(fairScore).as("FAIR >= POOR").isGreaterThanOrEqualTo(poorScore);
            assertThat(poorScore).as("POOR >= UNKNOWN").isGreaterThanOrEqualTo(unknownScore);
        }

        @Test
        void testGetScore_RatingOrderingInvariant_MultipleConfidenceLevels() {
            // Rating ordering should hold at all confidence levels
            for (double confidence : new double[]{0.0, 0.25, 0.5, 0.75, 1.0}) {
                int goodScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, confidence).getScore();
                int fairScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.FAIR, confidence).getScore();
                int poorScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.POOR, confidence).getScore();
                int unknownScore = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.UNKNOWN, confidence).getScore();

                assertThat(goodScore)
                        .as("At %.2f confidence: GOOD >= FAIR", confidence)
                        .isGreaterThanOrEqualTo(fairScore);
                assertThat(fairScore)
                        .as("At %.2f confidence: FAIR >= POOR", confidence)
                        .isGreaterThanOrEqualTo(poorScore);
                assertThat(poorScore)
                        .as("At %.2f confidence: POOR >= UNKNOWN", confidence)
                        .isGreaterThanOrEqualTo(unknownScore);
            }
        }

        @Test
        void testGetScore_ConfidenceScalingInvariant() {
            // Invariant: Higher confidence -> higher or equal score for same rating
            for (BandConditionRating rating : BandConditionRating.values()) {
                int scoreAtZero = new BandCondition(FrequencyBand.BAND_20M, rating, 0.0).getScore();
                int scoreAtHalf = new BandCondition(FrequencyBand.BAND_20M, rating, 0.5).getScore();
                int scoreAtFull = new BandCondition(FrequencyBand.BAND_20M, rating, 1.0).getScore();

                assertThat(scoreAtFull)
                        .as("%s: full confidence >= half confidence", rating)
                        .isGreaterThanOrEqualTo(scoreAtHalf);
                assertThat(scoreAtHalf)
                        .as("%s: half confidence >= zero confidence", rating)
                        .isGreaterThanOrEqualTo(scoreAtZero);
            }
        }
    }

    // ==========================================================================
    // Category 5: getScore() Boundary Tests (true boundaries only)
    // ==========================================================================

    @Nested
    class ScoreBoundaryTests {

        @Test
        void testGetScore_GoodAtFullConfidence_ReturnsMaxScore() {
            var condition = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.0);
            assertThat(condition.getScore())
                    .as("GOOD at full confidence should be maximum score")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_UnknownRating_AlwaysReturnsMinScore() {
            // UNKNOWN rating should always be 0 regardless of confidence
            for (double confidence : new double[]{0.0, 0.5, 1.0}) {
                var condition = new BandCondition(FrequencyBand.BAND_160M, BandConditionRating.UNKNOWN, confidence);
                assertThat(condition.getScore())
                        .as("UNKNOWN at %.2f confidence should be minimum", confidence)
                        .isEqualTo(MIN_SCORE);
            }
        }

        @Test
        void testGetScore_ZeroConfidence_ReturnsMinScore() {
            // Zero confidence should yield zero score for all ratings
            for (BandConditionRating rating : BandConditionRating.values()) {
                var condition = new BandCondition(FrequencyBand.BAND_20M, rating, 0.0);
                assertThat(condition.getScore())
                        .as("%s at zero confidence should be minimum", rating)
                        .isEqualTo(MIN_SCORE);
            }
        }
    }
}
