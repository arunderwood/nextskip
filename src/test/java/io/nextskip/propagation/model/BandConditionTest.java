package io.nextskip.propagation.model;

import io.nextskip.common.model.FrequencyBand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testIsFavorable_GoodWithHighConfidence() {
        // GOOD rating with confidence > 0.5 should be favorable
        var condition = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.6);
        assertTrue(condition.isFavorable());

        var condition2 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.0);
        assertTrue(condition2.isFavorable());
    }

    @Test
    void testIsFavorable_GoodWithLowConfidence() {
        // GOOD rating with confidence <= 0.5 should NOT be favorable
        var condition05 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.5);
        assertFalse(condition05.isFavorable()); // 0.5 is NOT > 0.5

        var condition03 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.3);
        assertFalse(condition03.isFavorable());
    }

    @Test
    void testIsFavorable_FairRating() {
        // FAIR rating should never be favorable, regardless of confidence
        var condition = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR, 1.0);
        assertFalse(condition.isFavorable());

        var condition2 = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.7);
        assertFalse(condition2.isFavorable());
    }

    @Test
    void testIsFavorable_PoorRating() {
        // POOR rating should never be favorable, regardless of confidence
        var condition = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 1.0);
        assertFalse(condition.isFavorable());

        var condition2 = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 0.1);
        assertFalse(condition2.isFavorable());
    }

    // ==========================================================================
    // Category 4: getScore() - All Ratings with Confidence Variations
    // ==========================================================================

    @Test
    void testGetScore_GoodRating() {
        // GOOD rating: score = 100 * confidence
        var condition1 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 1.0);
        assertEquals(100, condition1.getScore()); // 100 * 1.0 = 100

        var condition07 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.7);
        assertEquals(70, condition07.getScore()); // 100 * 0.7 = 70

        var condition05 = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.5);
        assertEquals(50, condition05.getScore()); // 100 * 0.5 = 50
    }

    @Test
    void testGetScore_FairRating() {
        // FAIR rating: score = 60 * confidence
        var condition1 = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR, 1.0);
        assertEquals(60, condition1.getScore()); // 60 * 1.0 = 60

        var condition05 = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.5);
        assertEquals(30, condition05.getScore()); // 60 * 0.5 = 30

        var condition08 = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.8);
        assertEquals(48, condition08.getScore()); // 60 * 0.8 = 48
    }

    @Test
    void testGetScore_PoorRating() {
        // POOR rating: score = 20 * confidence
        var condition1 = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 1.0);
        assertEquals(20, condition1.getScore()); // 20 * 1.0 = 20

        var condition05 = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 0.5);
        assertEquals(10, condition05.getScore()); // 20 * 0.5 = 10

        var condition02 = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 0.2);
        assertEquals(4, condition02.getScore()); // 20 * 0.2 = 4
    }

    @Test
    void testGetScore_UnknownRating() {
        // UNKNOWN rating: score always 0, regardless of confidence
        var condition1 = new BandCondition(FrequencyBand.BAND_160M, BandConditionRating.UNKNOWN, 1.0);
        assertEquals(0, condition1.getScore());

        var condition0 = new BandCondition(FrequencyBand.BAND_160M, BandConditionRating.UNKNOWN, 0.0);
        assertEquals(0, condition0.getScore());

        var condition05 = new BandCondition(FrequencyBand.BAND_160M, BandConditionRating.UNKNOWN, 0.5);
        assertEquals(0, condition05.getScore());
    }

    @Test
    void testGetScore_PartialConfidence() {
        // Verify score calculation with various partial confidence values
        var goodPartial = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.75);
        assertEquals(75, goodPartial.getScore()); // 100 * 0.75 = 75

        var fairPartial = new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.33);
        assertEquals(19, fairPartial.getScore()); // 60 * 0.33 = 19.8 → 19 (int cast)

        var poorPartial = new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.POOR, 0.9);
        assertEquals(18, poorPartial.getScore()); // 20 * 0.9 = 18
    }
}
