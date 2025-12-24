package io.nextskip.propagation.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testGetGeomagneticActivity_Quiet() {
        // K-index 0-2 should return "Quiet"
        var indices0 = new SolarIndices(100.0, 10, 0, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Quiet", indices0.getGeomagneticActivity());

        var indices1 = new SolarIndices(100.0, 10, 1, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Quiet", indices1.getGeomagneticActivity());

        var indices2 = new SolarIndices(100.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Quiet", indices2.getGeomagneticActivity());
    }

    @Test
    void testGetGeomagneticActivity_Unsettled() {
        // K-index 3-4 should return "Unsettled"
        var indices3 = new SolarIndices(100.0, 10, 3, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Unsettled", indices3.getGeomagneticActivity());

        var indices4 = new SolarIndices(100.0, 10, 4, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Unsettled", indices4.getGeomagneticActivity());
    }

    @Test
    void testGetGeomagneticActivity_Active() {
        // K-index 5-6 should return "Active"
        var indices5 = new SolarIndices(100.0, 10, 5, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Active", indices5.getGeomagneticActivity());

        var indices6 = new SolarIndices(100.0, 10, 6, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Active", indices6.getGeomagneticActivity());
    }

    @Test
    void testGetGeomagneticActivity_Storm() {
        // K-index 7-8 should return "Storm"
        var indices7 = new SolarIndices(100.0, 10, 7, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Storm", indices7.getGeomagneticActivity());

        var indices8 = new SolarIndices(100.0, 10, 8, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Storm", indices8.getGeomagneticActivity());
    }

    @Test
    void testGetGeomagneticActivity_SevereStorm() {
        // K-index 9+ should return "Severe Storm"
        var indices9 = new SolarIndices(100.0, 10, 9, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Severe Storm", indices9.getGeomagneticActivity());
    }

    // ==========================================================================
    // Category 2: getSolarFluxLevel() - SFI to Flux Level Mapping
    // ==========================================================================

    @Test
    void testGetSolarFluxLevel_VeryLow() {
        // SFI < 70 should return "Very Low"
        var indices50 = new SolarIndices(50.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Very Low", indices50.getSolarFluxLevel());

        var indices69 = new SolarIndices(69.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Very Low", indices69.getSolarFluxLevel());
    }

    @Test
    void testGetSolarFluxLevel_Low() {
        // SFI 70-99 should return "Low"
        var indices70 = new SolarIndices(70.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Low", indices70.getSolarFluxLevel());

        var indices85 = new SolarIndices(85.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Low", indices85.getSolarFluxLevel());

        var indices99 = new SolarIndices(99.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Low", indices99.getSolarFluxLevel());
    }

    @Test
    void testGetSolarFluxLevel_Moderate() {
        // SFI 100-149 should return "Moderate"
        var indices100 = new SolarIndices(100.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Moderate", indices100.getSolarFluxLevel());

        var indices125 = new SolarIndices(125.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Moderate", indices125.getSolarFluxLevel());

        var indices149 = new SolarIndices(149.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Moderate", indices149.getSolarFluxLevel());
    }

    @Test
    void testGetSolarFluxLevel_High() {
        // SFI 150-199 should return "High"
        var indices150 = new SolarIndices(150.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("High", indices150.getSolarFluxLevel());

        var indices175 = new SolarIndices(175.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("High", indices175.getSolarFluxLevel());

        var indices199 = new SolarIndices(199.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("High", indices199.getSolarFluxLevel());
    }

    @Test
    void testGetSolarFluxLevel_VeryHigh() {
        // SFI >= 200 should return "Very High"
        var indices200 = new SolarIndices(200.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Very High", indices200.getSolarFluxLevel());

        var indices250 = new SolarIndices(250.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Very High", indices250.getSolarFluxLevel());

        var indices300 = new SolarIndices(300.0, 10, 2, 50, Instant.now(), TEST_SOURCE);
        assertEquals("Very High", indices300.getSolarFluxLevel());
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
