package io.nextskip.common.model;

import io.nextskip.common.util.HamRadioUtils;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite for GridSquare.java.
 *
 * Verifies the custom Maidenhead Grid Square Locator System implementation
 * against reference implementations and the IARU specification.
 *
 * Test categories:
 * 1. Constructor Validation (5 tests)
 * 2. Reference Test Cases (4 tests) - Verified against Giangrandi QTH Calculator
 * 3. All Precision Levels (4 tests)
 * 4. Edge Cases (6 tests)
 * 5. Round-Trip Conversion (4 tests)
 * 6. Precision Metadata (5 tests)
 * 7. Integration with HamRadioUtils (3 tests)
 */
@SuppressWarnings("PMD.TooManyMethods") // Comprehensive test suite requires many test methods
class GridSquareTest {

    // Test data constants to avoid duplicate literals
    private static final String GRID_CN87 = "CN87";
    private static final String GRID_CN87TS = "CN87ts";
    private static final String GRID_CN87TS50 = "CN87ts50";
    private static final String ERROR_NULL_OR_BLANK = "cannot be null or blank";
    private static final String ERROR_INVALID_LENGTH = "must be 2, 4, 6, or 8 characters";

    // Reference test cases from Giangrandi QTH Calculator
    private static final TestCase MUNICH = new TestCase("JN58rj", 48.396, 11.458);
    private static final TestCase SOUTH_AFRICA = new TestCase("KF29oh", -30.688, 25.208);
    private static final TestCase SOUTH_AMERICA = new TestCase("FE62es", -47.229, -67.625);
    private static final TestCase NORTH_AMERICA = new TestCase("DN15ga", 45.021, -117.458);

    // ==========================================================================
    // Category 1: Constructor Validation (5 tests)
    // ==========================================================================

    @Test
    void testConstructor_ValidGridSquares() {
        // Valid grid squares at all precision levels
        assertDoesNotThrow(() -> new GridSquare("CN"));         // 2-char field
        assertDoesNotThrow(() -> new GridSquare(GRID_CN87));       // 4-char square
        assertDoesNotThrow(() -> new GridSquare(GRID_CN87TS));     // 6-char subsquare
        assertDoesNotThrow(() -> new GridSquare(GRID_CN87TS50));   // 8-char extended

        // Verify values are stored
        assertEquals("CN", new GridSquare("CN").value());
        assertEquals(GRID_CN87, new GridSquare(GRID_CN87).value());
        assertEquals("CN87TS", new GridSquare(GRID_CN87TS).value()); // Normalized to uppercase
        assertEquals("CN87TS50", new GridSquare(GRID_CN87TS50).value());
    }

    @Test
    void testConstructor_NullValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare(null)
        );
        assertTrue(exception.getMessage().contains(ERROR_NULL_OR_BLANK));
    }

    @Test
    void testConstructor_BlankValue() {
        // Empty string
        IllegalArgumentException exception1 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("")
        );
        assertTrue(exception1.getMessage().contains(ERROR_NULL_OR_BLANK));

        // Whitespace only
        IllegalArgumentException exception2 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("   ")
        );
        assertTrue(exception2.getMessage().contains(ERROR_NULL_OR_BLANK));
    }

    @Test
    void testConstructor_InvalidLength() {
        // 1 character - too short
        IllegalArgumentException e1 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("C")
        );
        assertTrue(e1.getMessage().contains(ERROR_INVALID_LENGTH));

        // 3 characters - invalid
        IllegalArgumentException e2 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("CN8")
        );
        assertTrue(e2.getMessage().contains(ERROR_INVALID_LENGTH));

        // 5 characters - invalid
        IllegalArgumentException e3 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("CN87t")
        );
        assertTrue(e3.getMessage().contains(ERROR_INVALID_LENGTH));

        // 7 characters - invalid
        IllegalArgumentException e4 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("CN87ts5")
        );
        assertTrue(e4.getMessage().contains(ERROR_INVALID_LENGTH));

        // 9 characters - too long
        IllegalArgumentException e5 = assertThrows(
                IllegalArgumentException.class,
                () -> new GridSquare("CN87ts500")
        );
        assertTrue(e5.getMessage().contains(ERROR_INVALID_LENGTH));
    }

    @Test
    void testConstructor_Normalization() {
        // Lowercase should be normalized to uppercase
        GridSquare grid1 = new GridSquare("cn87");
        assertEquals(GRID_CN87, grid1.value());

        GridSquare grid2 = new GridSquare("fn31pr");
        assertEquals("FN31PR", grid2.value());

        GridSquare grid3 = new GridSquare("Jn58Rj00");
        assertEquals("JN58RJ00", grid3.value());
    }

    // ==========================================================================
    // Category 2: Reference Test Cases (4 tests)
    // Verified against Giangrandi QTH Calculator
    // ==========================================================================

    @Test
    void testToCoordinates_Munich_JN58rj() {
        GridSquare grid = new GridSquare(MUNICH.gridSquare());
        Coordinates coords = grid.toCoordinates();

        assertCoordinatesEqual(
                new Coordinates(MUNICH.latitude(), MUNICH.longitude()),
                coords,
                0.05  // ±0.05° tolerance for 6-character grids
        );
    }

    @Test
    void testToCoordinates_SouthAfrica_KF29oh() {
        GridSquare grid = new GridSquare(SOUTH_AFRICA.gridSquare());
        Coordinates coords = grid.toCoordinates();

        assertCoordinatesEqual(
                new Coordinates(SOUTH_AFRICA.latitude(), SOUTH_AFRICA.longitude()),
                coords,
                0.05
        );
    }

    @Test
    void testToCoordinates_SouthAmerica_FE62es() {
        GridSquare grid = new GridSquare(SOUTH_AMERICA.gridSquare());
        Coordinates coords = grid.toCoordinates();

        assertCoordinatesEqual(
                new Coordinates(SOUTH_AMERICA.latitude(), SOUTH_AMERICA.longitude()),
                coords,
                0.05
        );
    }

    @Test
    void testToCoordinates_NorthAmerica_DN15ga() {
        GridSquare grid = new GridSquare(NORTH_AMERICA.gridSquare());
        Coordinates coords = grid.toCoordinates();

        assertCoordinatesEqual(
                new Coordinates(NORTH_AMERICA.latitude(), NORTH_AMERICA.longitude()),
                coords,
                0.05
        );
    }

    // ==========================================================================
    // Category 3: All Precision Levels (4 tests)
    // ==========================================================================

    @Test
    void testToCoordinates_2Character_Field() {
        GridSquare grid = new GridSquare("JN");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        // JN field covers central Europe
        assertTrue(coords.latitude() >= 40 && coords.latitude() <= 60,
                "Field 'JN' latitude should be in range 40-60°");
        assertTrue(coords.longitude() >= 0 && coords.longitude() <= 20,
                "Field 'JN' longitude should be in range 0-20°");
    }

    @Test
    void testToCoordinates_4Character_Square() {
        GridSquare grid = new GridSquare("JN58");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        // JN58 square covers Munich area
        assertTrue(coords.latitude() >= 48 && coords.latitude() <= 49,
                "Square 'JN58' latitude should be in range 48-49°");
        assertTrue(coords.longitude() >= 11 && coords.longitude() <= 13,
                "Square 'JN58' longitude should be in range 11-13°");
    }

    @Test
    void testToCoordinates_6Character_Subsquare() {
        GridSquare grid = new GridSquare("JN58rj");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        // JN58rj subsquare covers Munich city center
        assertTrue(coords.latitude() >= 48.3 && coords.latitude() <= 48.5,
                "Subsquare 'JN58rj' latitude should be in range 48.3-48.5°");
        assertTrue(coords.longitude() >= 11.4 && coords.longitude() <= 11.6,
                "Subsquare 'JN58rj' longitude should be in range 11.4-11.6°");
    }

    @Test
    void testToCoordinates_8Character_Extended() {
        GridSquare grid = new GridSquare("JN58rj00");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        // JN58rj00 is very precise
        assertTrue(coords.latitude() >= 48.35 && coords.latitude() <= 48.45,
                "Extended 'JN58rj00' latitude should be in range 48.35-48.45°");
        assertTrue(coords.longitude() >= 11.40 && coords.longitude() <= 11.50,
                "Extended 'JN58rj00' longitude should be in range 11.40-11.50°");
    }

    // ==========================================================================
    // Category 4: Edge Cases (6 tests)
    // ==========================================================================

    @Test
    void testToCoordinates_NorthPole_Area() {
        // RR is the northernmost field (lat 80-90°, lon 160-180°)
        GridSquare grid = new GridSquare("RR99xx99");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        assertTrue(coords.latitude() >= 80 && coords.latitude() <= 90,
                "North pole area should have latitude 80-90°");
        assertTrue(coords.longitude() >= 160 && coords.longitude() <= 180,
                "North pole area should have longitude 160-180°");
    }

    @Test
    void testToCoordinates_SouthPole_Area() {
        // AA is the southernmost field (lat -90 to -80°, lon -180 to -160°)
        GridSquare grid = new GridSquare("AA00aa00");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        assertTrue(coords.latitude() >= -90 && coords.latitude() <= -80,
                "South pole area should have latitude -90 to -80°");
        assertTrue(coords.longitude() >= -180 && coords.longitude() <= -160,
                "South pole area should have longitude -180 to -160°");
    }

    @Test
    void testToCoordinates_DateLine_West() {
        // AA is near -180° longitude (western date line)
        GridSquare grid = new GridSquare("AA00");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        assertTrue(coords.longitude() >= -180 && coords.longitude() <= -160,
                "Western date line should have longitude -180 to -160°");
    }

    @Test
    void testToCoordinates_DateLine_East() {
        // RR is near +180° longitude (eastern date line)
        GridSquare grid = new GridSquare("RR00");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        assertTrue(coords.longitude() >= 160 && coords.longitude() <= 180,
                "Eastern date line should have longitude 160-180°");
    }

    @Test
    void testToCoordinates_Equator() {
        // Field starting with 'J' or 'K' crosses the equator
        GridSquare grid = new GridSquare("JJ00");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        assertTrue(coords.latitude() >= -10 && coords.latitude() <= 10,
                "Equator region should have latitude -10 to 10°");
    }

    @Test
    void testToCoordinates_PrimeMeridian() {
        // Field 'J' spans 0° longitude (Greenwich)
        GridSquare grid = new GridSquare("JJ00");
        Coordinates coords = grid.toCoordinates();

        assertNotNull(coords);
        assertTrue(coords.longitude() >= -10 && coords.longitude() <= 10,
                "Prime meridian region should have longitude -10 to 10°");
    }

    // ==========================================================================
    // Category 5: Round-Trip Conversion (4 tests)
    // ==========================================================================

    @Test
    void testRoundTrip_4Character() {
        String originalGrid = GRID_CN87;
        GridSquare grid1 = new GridSquare(originalGrid);
        Coordinates coords = grid1.toCoordinates();

        // Convert back to grid square (4-char precision)
        GridSquare roundTripGrid = HamRadioUtils.coordinatesToGridSquare(coords, 4);

        assertEquals(originalGrid, roundTripGrid.value(),
                "Round-trip conversion should preserve 4-character grid");
    }

    @Test
    void testRoundTrip_6Character() {
        String originalGrid = GRID_CN87TS;
        GridSquare grid1 = new GridSquare(originalGrid);
        Coordinates coords = grid1.toCoordinates();

        // Convert back to grid square (6-char precision)
        GridSquare roundTripGrid = HamRadioUtils.coordinatesToGridSquare(coords, 6);

        assertEquals(originalGrid.toUpperCase(Locale.ROOT), roundTripGrid.value(),
                "Round-trip conversion should preserve 6-character grid");
    }

    @Test
    void testRoundTrip_8Character() {
        String originalGrid = GRID_CN87TS50;
        GridSquare grid1 = new GridSquare(originalGrid);
        Coordinates coords = grid1.toCoordinates();

        // Convert back to grid square (8-char precision)
        GridSquare roundTripGrid = HamRadioUtils.coordinatesToGridSquare(coords, 8);

        assertEquals(originalGrid.toUpperCase(Locale.ROOT), roundTripGrid.value(),
                "Round-trip conversion should preserve 8-character grid");
    }

    @Test
    void testRoundTrip_KnownLocations() {
        assertAll("Round trip for known locations",
            // Seattle
            () -> testRoundTripLocation(47.6062, -122.3321, "CN87ts"),
            // New York
            () -> testRoundTripLocation(40.7128, -74.0060, "FN20xr"),
            // London
            () -> testRoundTripLocation(51.5074, -0.1278, "IO91wm"),
            // Tokyo
            () -> testRoundTripLocation(35.6762, 139.6503, "PM95vq")
        );
    }

    // ==========================================================================
    // Category 6: Precision Metadata (5 tests)
    // ==========================================================================

    @Test
    void testPrecisionKm_2Character() {
        GridSquare grid = new GridSquare("CN");
        assertEquals(1000.0, grid.precisionKm(), 0.01,
                "2-character field should have ~1000 km precision");
    }

    @Test
    void testPrecisionKm_4Character() {
        GridSquare grid = new GridSquare(GRID_CN87);
        assertEquals(100.0, grid.precisionKm(), 0.01,
                "4-character square should have ~100 km precision");
    }

    @Test
    void testPrecisionKm_6Character() {
        GridSquare grid = new GridSquare(GRID_CN87TS);
        assertEquals(5.0, grid.precisionKm(), 0.01,
                "6-character subsquare should have ~5 km precision");
    }

    @Test
    void testPrecisionKm_8Character() {
        GridSquare grid = new GridSquare(GRID_CN87TS50);
        assertEquals(0.5, grid.precisionKm(), 0.01,
                "8-character extended should have ~500 m precision");
    }

    @Test
    void testPrecisionKm_Consistency() {
        // Precision should decrease (more precise) with more characters
        double precision2 = new GridSquare("CN").precisionKm();
        double precision4 = new GridSquare(GRID_CN87).precisionKm();
        double precision6 = new GridSquare(GRID_CN87TS).precisionKm();
        double precision8 = new GridSquare(GRID_CN87TS50).precisionKm();

        assertTrue(precision2 > precision4,
                "2-char precision should be less precise than 4-char");
        assertTrue(precision4 > precision6,
                "4-char precision should be less precise than 6-char");
        assertTrue(precision6 > precision8,
                "6-char precision should be less precise than 8-char");
    }

    // ==========================================================================
    // Category 7: Integration with HamRadioUtils (3 tests)
    // ==========================================================================

    @Test
    void testIntegrationWithHamRadioUtils_Seattle() {
        // Seattle coordinates
        double lat = 47.6062;
        double lon = -122.3321;

        // Convert coords → grid (6-char precision)
        GridSquare gridSquare = HamRadioUtils.coordinatesToGridSquare(
                new Coordinates(lat, lon), 6);

        // Convert grid → coords
        Coordinates coords = gridSquare.toCoordinates();

        // Should be within the same grid square (~5 km tolerance)
        assertCoordinatesEqual(new Coordinates(lat, lon), coords, 0.05);
    }

    @Test
    void testIntegrationWithHamRadioUtils_AllPrecisions() {
        double lat = 48.396;
        double lon = 11.458;
        Coordinates inputCoords = new Coordinates(lat, lon);

        // Test each precision level
        for (int precision : new int[]{4, 6, 8}) {
            GridSquare gridSquare = HamRadioUtils.coordinatesToGridSquare(inputCoords, precision);
            Coordinates coords = gridSquare.toCoordinates();

            assertNotNull(coords);
            // Verify we're in the same general area
            assertEquals(lat, coords.latitude(), 1.0,
                    "Latitude should be within 1° for precision " + precision);
            assertEquals(lon, coords.longitude(), 1.0,
                    "Longitude should be within 1° for precision " + precision);
        }
    }

    @Test
    void testIntegrationWithHamRadioUtils_EdgeCases() {
        assertAll("Edge cases for HamRadioUtils integration",
            // North pole area
            () -> testIntegrationEdgeCase(85.0, 170.0),
            // South pole area
            () -> testIntegrationEdgeCase(-85.0, -170.0),
            // Date line
            () -> testIntegrationEdgeCase(0.0, 179.0),
            () -> testIntegrationEdgeCase(0.0, -179.0),
            // Equator
            () -> testIntegrationEdgeCase(0.0, 0.0)
        );
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    /**
     * Assert that two Coordinates are equal within a tolerance.
     */
    private void assertCoordinatesEqual(Coordinates expected, Coordinates actual, double tolerance) {
        assertEquals(expected.latitude(), actual.latitude(), tolerance,
                "Latitude mismatch");
        assertEquals(expected.longitude(), actual.longitude(), tolerance,
                "Longitude mismatch");
    }

    /**
     * Test round-trip conversion for a specific location.
     */
    private void testRoundTripLocation(double lat, double lon, String expectedGridPrefix) {
        GridSquare gridSquare = HamRadioUtils.coordinatesToGridSquare(
                new Coordinates(lat, lon), 6);
        assertTrue(gridSquare.value().startsWith(expectedGridPrefix.substring(0, 4)),
                String.format("Grid for %.4f, %.4f should start with %s, got %s",
                        lat, lon, expectedGridPrefix.substring(0, 4), gridSquare.value()));

        Coordinates coords = gridSquare.toCoordinates();

        // Should be within the same grid square
        assertCoordinatesEqual(new Coordinates(lat, lon), coords, 0.05);
    }

    /**
     * Test integration with HamRadioUtils for edge case coordinates.
     */
    private void testIntegrationEdgeCase(double lat, double lon) {
        GridSquare gridSquare = HamRadioUtils.coordinatesToGridSquare(
                new Coordinates(lat, lon), 6);
        assertNotNull(gridSquare);

        Coordinates coords = gridSquare.toCoordinates();

        assertNotNull(coords);
        // Edge cases may have larger variation, so use 1° tolerance
        assertEquals(lat, coords.latitude(), 1.0,
                String.format("Latitude for edge case %.2f, %.2f", lat, lon));
        assertEquals(lon, coords.longitude(), 1.0,
                String.format("Longitude for edge case %.2f, %.2f", lat, lon));
    }

    /**
     * Test case record for reference implementations.
     */
    record TestCase(String gridSquare, double latitude, double longitude) { }
}
