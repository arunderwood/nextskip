package io.nextskip.propagation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nextskip.common.model.FrequencyBand;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive test suite for BandCondition record.
 *
 * Tests the core scoring system used for dashboard card prioritization:
 * - Constructor validation (confidence 0.0-1.0)
 * - isFavorable() logic (GOOD rating && confidence > 0.5)
 * - getScore() calculation (rating × confidence)
 *
 * Test categories:
 * 1. Constructor validation - confidence range (3 tests)
 * 2. Secondary constructors - defaults (2 tests)
 * 3. isFavorable() - rating and confidence combinations (4 tests)
 * 4. getScore() - all ratings with confidence variations (5 tests)
 */
class BandConditionTest {

    // ==========================================================================
    // Category 1: Constructor Validation - Confidence Range
    // ==========================================================================

    @Test
    void testConstructor_ValidConfidence() {
        // Confidence at boundaries and mid-range should be valid
        var condition0 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.0, null);
        assertEquals(0.0, condition0.confidence());

        var condition05 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.5, null);
        assertEquals(0.5, condition05.confidence());

        var condition1 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.0, null);
        assertEquals(1.0, condition1.confidence());
    }

    @Test
    void testConstructor_ConfidenceBelowZero_ThrowsException() {
        // Confidence below 0.0 should throw IllegalArgumentException
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, -0.001, null)
        );
        assertTrue(exception.getMessage().contains("Confidence must be between 0.0 and 1.0"));
    }

    @Test
    void testConstructor_ConfidenceAboveOne_ThrowsException() {
        // Confidence above 1.0 should throw IllegalArgumentException
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.001, null)
        );
        assertTrue(exception.getMessage().contains("Confidence must be between 0.0 and 1.0"));
    }

    // ==========================================================================
    // Category 2: Secondary Constructors - Defaults
    // ==========================================================================

    @Test
    void testTwoArgConstructor_DefaultsConfidenceToOne() {
        // 2-arg constructor should default confidence to 1.0 and notes to null
        var condition = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR);

        assertEquals(FrequencyBand.BAND_40M, condition.band());
        assertEquals(BandConditionRating.FAIR, condition.rating());
        assertEquals(1.0, condition.confidence());
        assertNull(condition.notes());
    }

    @Test
    void testThreeArgConstructor_SetsConfidenceNoNotes() {
        // 3-arg constructor should set confidence and notes to null
        var condition = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 0.7);

        assertEquals(FrequencyBand.BAND_80M, condition.band());
        assertEquals(BandConditionRating.POOR, condition.rating());
        assertEquals(0.7, condition.confidence());
        assertNull(condition.notes());
    }

    // ==========================================================================
    // Category 3: isFavorable() - Rating and Confidence Combinations
    // ==========================================================================

    static Stream<Arguments> isFavorableProvider() {
        return Stream.of(
                // GOOD rating with confidence > 0.5 should be favorable
                Arguments.of(BandConditionRating.GOOD, 0.6, true),
                Arguments.of(BandConditionRating.GOOD, 1.0, true),
                // GOOD rating with confidence <= 0.5 should NOT be favorable
                Arguments.of(BandConditionRating.GOOD, 0.5, false),
                Arguments.of(BandConditionRating.GOOD, 0.3, false),
                // FAIR rating should never be favorable
                Arguments.of(BandConditionRating.FAIR, 1.0, false),
                Arguments.of(BandConditionRating.FAIR, 0.7, false),
                // POOR rating should never be favorable
                Arguments.of(BandConditionRating.POOR, 1.0, false),
                Arguments.of(BandConditionRating.POOR, 0.1, false),
                // UNKNOWN rating should never be favorable
                Arguments.of(BandConditionRating.UNKNOWN, 1.0, false)
        );
    }

    @ParameterizedTest(name = "{0} with confidence {1} -> favorable={2}")
    @MethodSource("isFavorableProvider")
    void testIsFavorable_RatingWithConfidence(
            BandConditionRating rating, double confidence, boolean expectedFavorable) {
        var condition = new BandCondition(FrequencyBand.BAND_20M, rating, confidence);

        if (expectedFavorable) {
            assertTrue(condition.isFavorable(),
                    () -> rating + " with confidence " + confidence + " should be favorable");
        } else {
            assertFalse(condition.isFavorable(),
                    () -> rating + " with confidence " + confidence + " should NOT be favorable");
        }
    }

    // ==========================================================================
    // Category 4: getScore() - All Ratings with Confidence Variations
    // ==========================================================================

    @ParameterizedTest(name = "{0} with confidence {1} -> score {2}")
    @CsvSource({
            // GOOD rating: score = 100 * confidence
            "GOOD, 1.0, 100",
            "GOOD, 0.7, 70",
            "GOOD, 0.5, 50",
            "GOOD, 0.75, 75",
            // FAIR rating: score = 60 * confidence
            "FAIR, 1.0, 60",
            "FAIR, 0.5, 30",
            "FAIR, 0.8, 48",
            "FAIR, 0.33, 19",
            // POOR rating: score = 20 * confidence
            "POOR, 1.0, 20",
            "POOR, 0.5, 10",
            "POOR, 0.2, 4",
            "POOR, 0.9, 18",
            // UNKNOWN rating: score always 0
            "UNKNOWN, 1.0, 0",
            "UNKNOWN, 0.0, 0",
            "UNKNOWN, 0.5, 0"
    })
    void testGetScore_RatingWithConfidence(
            BandConditionRating rating, double confidence, int expectedScore) {
        var condition = new BandCondition(FrequencyBand.BAND_20M, rating, confidence);

        assertEquals(expectedScore, condition.getScore(),
                () -> rating + " with confidence " + confidence + " should have score " + expectedScore);
    }
}
