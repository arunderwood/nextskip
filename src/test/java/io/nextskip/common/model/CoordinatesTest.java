package io.nextskip.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite for Coordinates record.
 *
 * Tests geographic coordinate validation and Haversine distance calculations:
 * - Latitude validation (-90 to +90 degrees)
 * - Longitude validation (-180 to +180 degrees)
 * - distanceTo() Haversine formula with known reference distances
 *
 * Test categories:
 * 1. Latitude validation (3 tests)
 * 2. Longitude validation (3 tests)
 * 3. distanceTo() with known distances (5 tests)
 */
class CoordinatesTest {

    // Reference coordinates for testing
    private static final Coordinates NEW_YORK = new Coordinates(40.7128, -74.0060);
    private static final Coordinates LONDON = new Coordinates(51.5074, -0.1278);
    private static final Coordinates TOKYO = new Coordinates(35.6762, 139.6503);
    private static final Coordinates LOS_ANGELES = new Coordinates(34.0522, -118.2437);
    private static final Coordinates NORTH_POLE = new Coordinates(90.0, 0.0);
    private static final Coordinates SOUTH_POLE = new Coordinates(-90.0, 0.0);
    private static final Coordinates EQUATOR_WEST = new Coordinates(0.0, -90.0);
    private static final Coordinates EQUATOR_EAST = new Coordinates(0.0, 90.0);

    // Distance tolerance for floating-point comparisons (in km)
    private static final double TOLERANCE_KM = 1.0;

    // ==========================================================================
    // Category 1: Latitude Validation
    // ==========================================================================

    @Test
    void testConstructor_ValidLatitude() {
        // Valid latitudes at boundaries and mid-range
        var south = new Coordinates(-90.0, 0.0);
        assertEquals(-90.0, south.latitude());

        var equator = new Coordinates(0.0, 0.0);
        assertEquals(0.0, equator.latitude());

        var north = new Coordinates(90.0, 0.0);
        assertEquals(90.0, north.latitude());
    }

    @Test
    void testConstructor_LatitudeBelowRange_ThrowsException() {
        // Latitude below -90 should throw IllegalArgumentException
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new Coordinates(-90.001, 0.0)
        );
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90 degrees"));
    }

    @Test
    void testConstructor_LatitudeAboveRange_ThrowsException() {
        // Latitude above 90 should throw IllegalArgumentException
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new Coordinates(90.001, 0.0)
        );
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90 degrees"));
    }

    // ==========================================================================
    // Category 2: Longitude Validation
    // ==========================================================================

    @Test
    void testConstructor_ValidLongitude() {
        // Valid longitudes at boundaries and mid-range
        var west = new Coordinates(0.0, -180.0);
        assertEquals(-180.0, west.longitude());

        var primeMeridian = new Coordinates(0.0, 0.0);
        assertEquals(0.0, primeMeridian.longitude());

        var east = new Coordinates(0.0, 180.0);
        assertEquals(180.0, east.longitude());
    }

    @Test
    void testConstructor_LongitudeBelowRange_ThrowsException() {
        // Longitude below -180 should throw IllegalArgumentException
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new Coordinates(0.0, -180.001)
        );
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180 degrees"));
    }

    @Test
    void testConstructor_LongitudeAboveRange_ThrowsException() {
        // Longitude above 180 should throw IllegalArgumentException
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new Coordinates(0.0, 180.001)
        );
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180 degrees"));
    }

    // ==========================================================================
    // Category 3: distanceTo() with Known Reference Distances
    // ==========================================================================

    @Test
    void testDistanceTo_SamePoint() {
        // Distance from a point to itself should be 0
        double distance = NEW_YORK.distanceTo(NEW_YORK);
        assertEquals(0.0, distance, TOLERANCE_KM);

        double distance2 = LONDON.distanceTo(LONDON);
        assertEquals(0.0, distance2, TOLERANCE_KM);
    }

    @Test
    void testDistanceTo_NewYorkToLondon() {
        // New York to London: approximately 5,570 km
        // Reference: Multiple geographic calculators
        double distance = NEW_YORK.distanceTo(LONDON);
        assertEquals(5570.0, distance, 50.0); // ±50 km tolerance

        // Distance should be symmetric
        double reverseDistance = LONDON.distanceTo(NEW_YORK);
        assertEquals(distance, reverseDistance, TOLERANCE_KM);
    }

    @Test
    void testDistanceTo_NorthPoleToSouthPole() {
        // North Pole to South Pole: approximately half of Earth's circumference
        // Earth's circumference ≈ 40,075 km, so half ≈ 20,037 km
        double distance = NORTH_POLE.distanceTo(SOUTH_POLE);
        assertEquals(20037.0, distance, 50.0); // ±50 km tolerance
    }

    @Test
    void testDistanceTo_CrossingDateLine() {
        // Tokyo to Los Angeles: crosses the International Date Line
        // Approximate distance: 8,800 km
        double distance = TOKYO.distanceTo(LOS_ANGELES);
        assertEquals(8800.0, distance, 100.0); // ±100 km tolerance

        // Distance should be symmetric
        double reverseDistance = LOS_ANGELES.distanceTo(TOKYO);
        assertEquals(distance, reverseDistance, TOLERANCE_KM);
    }

    @Test
    void testDistanceTo_CrossingEquator() {
        // Two points on the equator, 180 degrees apart
        // Should be half the circumference at the equator ≈ 20,037 km
        double distance = EQUATOR_WEST.distanceTo(EQUATOR_EAST);
        assertEquals(20037.0, distance, 50.0); // ±50 km tolerance
    }
}
