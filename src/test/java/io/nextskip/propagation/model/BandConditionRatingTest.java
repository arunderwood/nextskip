package io.nextskip.propagation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprehensive test suite for BandConditionRating enum.
 *
 * Tests the string parsing logic for band condition ratings,
 * ensuring robust handling of various input formats.
 *
 * Test categories:
 * 1. fromString() - Valid values (1 test)
 * 2. fromString() - Case insensitivity (1 test)
 * 3. fromString() - Null input (1 test)
 * 4. fromString() - Blank input (1 test)
 * 5. fromString() - Invalid input (1 test)
 */
class BandConditionRatingTest {

    // ==========================================================================
    // Category 1: fromString() - Valid Values
    // ==========================================================================

    @Test
    void testFromString_ValidValues() {
        // All valid rating values should parse correctly
        assertEquals(BandConditionRating.GOOD, BandConditionRating.fromString("GOOD"));
        assertEquals(BandConditionRating.FAIR, BandConditionRating.fromString("FAIR"));
        assertEquals(BandConditionRating.POOR, BandConditionRating.fromString("POOR"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("UNKNOWN"));
    }

    // ==========================================================================
    // Category 2: fromString() - Case Insensitivity
    // ==========================================================================

    @Test
    void testFromString_CaseInsensitive() {
        // Parsing should be case-insensitive
        assertEquals(BandConditionRating.GOOD, BandConditionRating.fromString("good"));
        assertEquals(BandConditionRating.GOOD, BandConditionRating.fromString("Good"));
        assertEquals(BandConditionRating.GOOD, BandConditionRating.fromString("GOOD"));
        assertEquals(BandConditionRating.GOOD, BandConditionRating.fromString("gOoD"));

        assertEquals(BandConditionRating.FAIR, BandConditionRating.fromString("fair"));
        assertEquals(BandConditionRating.FAIR, BandConditionRating.fromString("Fair"));
        assertEquals(BandConditionRating.FAIR, BandConditionRating.fromString("FAIR"));

        assertEquals(BandConditionRating.POOR, BandConditionRating.fromString("poor"));
        assertEquals(BandConditionRating.POOR, BandConditionRating.fromString("Poor"));
        assertEquals(BandConditionRating.POOR, BandConditionRating.fromString("POOR"));

        // With whitespace (should be trimmed)
        assertEquals(BandConditionRating.GOOD, BandConditionRating.fromString(" GOOD "));
        assertEquals(BandConditionRating.FAIR, BandConditionRating.fromString("  fair  "));
        assertEquals(BandConditionRating.POOR, BandConditionRating.fromString("\tpoor\t"));
    }

    // ==========================================================================
    // Category 3: fromString() - Null Input
    // ==========================================================================

    @Test
    void testFromString_Null() {
        // Null input should return UNKNOWN
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString(null));
    }

    // ==========================================================================
    // Category 4: fromString() - Blank Input
    // ==========================================================================

    @Test
    void testFromString_Blank() {
        // Blank/empty strings should return UNKNOWN
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString(""));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("   "));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("\t"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("\n"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("  \t  "));
    }

    // ==========================================================================
    // Category 5: fromString() - Invalid Input
    // ==========================================================================

    @Test
    void testFromString_Invalid() {
        // Invalid strings should return UNKNOWN (fallback)
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("invalid"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("BAD"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("EXCELLENT"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("123"));
        assertEquals(BandConditionRating.UNKNOWN, BandConditionRating.fromString("G00D")); // Typo
    }
}
