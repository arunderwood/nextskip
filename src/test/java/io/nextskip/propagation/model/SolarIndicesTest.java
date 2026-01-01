package io.nextskip.propagation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Comprehensive test suite for SolarIndices record.
 *
 * Tests business logic methods for solar activity assessment:
 * - getGeomagneticActivity() - K-index to activity level mapping
 * - getSolarFluxLevel() - SFI to flux level mapping
 * - isFavorable() - Overall propagation favorability
 *
 * Test categories:
 * 1. getGeomagneticActivity() - 5 branches based on K-index (5 tests)
 * 2. getSolarFluxLevel() - 5 branches based on SFI (5 tests)
 * 3. isFavorable() - Boundary value testing (3 tests)
 */
class SolarIndicesTest {

    private static final String TEST_SOURCE = "Test";

    // ==========================================================================
    // Category 1: getGeomagneticActivity() - K-index to Activity Level Mapping
    // ==========================================================================

    @ParameterizedTest(name = "K-index {0} -> {1}")
    @CsvSource({
            // K-index 0-2 should return "Quiet"
            "0, Quiet",
            "1, Quiet",
            "2, Quiet",
            // K-index 3-4 should return "Unsettled"
            "3, Unsettled",
            "4, Unsettled",
            // K-index 5-6 should return "Active"
            "5, Active",
            "6, Active",
            // K-index 7-8 should return "Storm"
            "7, Storm",
            "8, Storm",
            // K-index 9+ should return "Severe Storm"
            "9, Severe Storm"
    })
    void testGetGeomagneticActivity_KIndexMapping(int kIndex, String expectedActivity) {
        var indices = new SolarIndices(100.0, 10, kIndex, 50, Instant.now(), TEST_SOURCE);

        assertEquals(expectedActivity, indices.getGeomagneticActivity(),
                () -> "K-index " + kIndex + " should map to " + expectedActivity);
    }

    // ==========================================================================
    // Category 2: getSolarFluxLevel() - SFI to Flux Level Mapping
    // ==========================================================================

    @ParameterizedTest(name = "SFI {0} -> {1}")
    @CsvSource({
            // SFI < 70 should return "Very Low"
            "50.0, Very Low",
            "69.0, Very Low",
            // SFI 70-99 should return "Low"
            "70.0, Low",
            "85.0, Low",
            "99.0, Low",
            // SFI 100-149 should return "Moderate"
            "100.0, Moderate",
            "125.0, Moderate",
            "149.0, Moderate",
            // SFI 150-199 should return "High"
            "150.0, High",
            "175.0, High",
            "199.0, High",
            // SFI >= 200 should return "Very High"
            "200.0, Very High",
            "250.0, Very High",
            "300.0, Very High"
    })
    void testGetSolarFluxLevel_SfiMapping(double sfi, String expectedLevel) {
        var indices = new SolarIndices(sfi, 10, 2, 50, Instant.now(), TEST_SOURCE);

        assertEquals(expectedLevel, indices.getSolarFluxLevel(),
                () -> "SFI " + sfi + " should map to " + expectedLevel);
    }

    // ==========================================================================
    // Category 3: isFavorable() - Overall Propagation Favorability
    // ==========================================================================

    @Test
    void testIsFavorable_AllConditionsMet() {
        // All favorable conditions: SFI > 100, K-index < 4, A-index < 20
        var favorable = new SolarIndices(150.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertTrue(favorable.isFavorable());

        // Just above all thresholds
        var barelyFavorable = new SolarIndices(101.0, 19, 3, 50, Instant.now(), TEST_SOURCE);
        assertTrue(barelyFavorable.isFavorable());
    }

    @Test
    void testIsFavorable_SfiBoundary() {
        // SFI boundary: solarFluxIndex must be > 100 (not >=)
        var atBoundary = new SolarIndices(100.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertFalse(atBoundary.isFavorable()); // 100 is NOT > 100

        var aboveBoundary = new SolarIndices(100.1, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertTrue(aboveBoundary.isFavorable()); // 100.1 IS > 100

        var belowBoundary = new SolarIndices(99.9, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertFalse(belowBoundary.isFavorable()); // 99.9 is NOT > 100
    }

    @Test
    void testIsFavorable_KIndexBoundary() {
        // K-index boundary: kIndex must be < 4 (not <=)
        var atBoundary = new SolarIndices(150.0, 10, 4, 50, Instant.now(), TEST_SOURCE);
        assertFalse(atBoundary.isFavorable()); // 4 is NOT < 4

        var belowBoundary = new SolarIndices(150.0, 10, 3, 50, Instant.now(), TEST_SOURCE);
        assertTrue(belowBoundary.isFavorable()); // 3 IS < 4

        var aboveBoundary = new SolarIndices(150.0, 10, 5, 50, Instant.now(), TEST_SOURCE);
        assertFalse(aboveBoundary.isFavorable()); // 5 is NOT < 4
    }

    @Test
    void testIsFavorable_AIndexBoundary() {
        // A-index boundary: aIndex must be < 20 (not <=)
        var atBoundary = new SolarIndices(150.0, 20, 2, 50, Instant.now(), TEST_SOURCE);
        assertFalse(atBoundary.isFavorable()); // 20 is NOT < 20

        var belowBoundary = new SolarIndices(150.0, 19, 2, 50, Instant.now(), TEST_SOURCE);
        assertTrue(belowBoundary.isFavorable()); // 19 IS < 20

        var aboveBoundary = new SolarIndices(150.0, 21, 2, 50, Instant.now(), TEST_SOURCE);
        assertFalse(aboveBoundary.isFavorable()); // 21 is NOT < 20
    }
}
