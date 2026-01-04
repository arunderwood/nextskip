package io.nextskip.common.util;

import io.nextskip.common.model.Coordinates;
import io.nextskip.common.model.GridSquare;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for HamRadioUtils.
 */
class HamRadioUtilsTest {

    @Test
    void shouldConvert_CoordinatesToGridSquare_4Characters() {
        // Seattle area: CN87
        Coordinates seattle = new Coordinates(47.6062, -122.3321);
        GridSquare grid = HamRadioUtils.coordinatesToGridSquare(seattle, 4);

        assertEquals("CN87", grid.value());
    }

    @Test
    void shouldConvert_CoordinatesToGridSquare_6Characters() {
        // Seattle area: should start with CN87
        Coordinates seattle = new Coordinates(47.6062, -122.3321);
        GridSquare grid = HamRadioUtils.coordinatesToGridSquare(seattle, 6);

        // Just verify it starts with the 4-character grid
        assertTrue(grid.value().startsWith("CN87"));
        assertEquals(6, grid.value().length());
    }

    @Test
    void shouldConvert_CoordinatesToGridSquare_Default6Chars() {
        Coordinates coords = new Coordinates(51.5074, -0.1278); // London
        GridSquare grid = HamRadioUtils.coordinatesToGridSquare(coords);

        assertEquals(6, grid.value().length());
        assertTrue(grid.value().startsWith("IO91"));
    }

    @Test
    void shouldConvert_GridSquareToCoordinates() {
        GridSquare grid = new GridSquare("FN31");
        Coordinates coords = grid.toCoordinates();

        // FN31 should be in the New England area (approximate center of grid square)
        // Field FN: lon -80 to -60, lat 40 to 50
        // Square 31: adds 2deg lon, 1deg lat offset, then centers
        assertTrue(coords.latitude() >= 40.0 && coords.latitude() <= 50.0);
        assertTrue(coords.longitude() >= -80.0 && coords.longitude() <= -60.0);
    }

    @Test
    void shouldCalculate_Bearing() {
        Coordinates seattle = new Coordinates(47.6062, -122.3321);
        Coordinates newYork = new Coordinates(40.7128, -74.0060);

        double bearing = HamRadioUtils.bearing(seattle, newYork);

        // Seattle to New York should be roughly east-southeast (around 90-120 degrees)
        assertTrue(bearing > 80 && bearing < 130, "Bearing was: " + bearing);
    }

    @Test
    void shouldCalculate_Bearing_NorthPole() {
        Coordinates origin = new Coordinates(0, 0);
        Coordinates north = new Coordinates(10, 0);

        double bearing = HamRadioUtils.bearing(origin, north);

        // Directly north should be 0 degrees
        assertTrue(bearing < 1 || bearing > 359);
    }

    @Test
    void shouldCalculate_Distance() {
        Coordinates seattle = new Coordinates(47.6062, -122.3321);
        Coordinates newYork = new Coordinates(40.7128, -74.0060);

        double distance = seattle.distanceTo(newYork);

        // Seattle to New York is approximately 3,900 km
        assertTrue(distance > 3800 && distance < 4000, "Distance was: " + distance);
    }

    @Test
    void shouldFormat_Frequency_MHz() {
        long freq = 14074000L; // 14.074 MHz
        String formatted = HamRadioUtils.formatFrequency(freq);

        assertEquals("14.074 MHz", formatted);
    }

    @Test
    void shouldFormat_Frequency_kHz() {
        long freq = 7200000L; // 7200 kHz = 7.2 MHz
        String formatted = HamRadioUtils.formatFrequency(freq);

        assertEquals("7.200 MHz", formatted);
    }

    @Test
    void shouldParse_Frequency_MHz() {
        Long freq = HamRadioUtils.parseFrequency("14.074 MHz");

        assertNotNull(freq);
        assertEquals(14074000L, freq);
    }

    @Test
    void shouldParse_Frequency_kHz() {
        Long freq = HamRadioUtils.parseFrequency("7200 kHz");

        assertNotNull(freq);
        assertEquals(7200000L, freq);
    }

    @Test
    void shouldParse_Frequency_NoSpace() {
        Long freq = HamRadioUtils.parseFrequency("145.500MHz");

        assertNotNull(freq);
        assertEquals(145500000L, freq);
    }

    @Test
    void shouldParse_Frequency_Invalid() {
        Long freq = HamRadioUtils.parseFrequency("invalid");

        assertNull(freq);
    }

    @Test
    void shouldParse_Frequency_Null() {
        Long freq = HamRadioUtils.parseFrequency(null);

        assertNull(freq);
    }

    @Test
    @SuppressWarnings("deprecation") // Testing deprecated method
    void shouldValidate_Callsign_Valid() {
        assertTrue(HamRadioUtils.isValidCallsign("W1AW"));
        assertTrue(HamRadioUtils.isValidCallsign("K5ABC"));
        assertTrue(HamRadioUtils.isValidCallsign("N2YO"));
        assertTrue(HamRadioUtils.isValidCallsign("VK2ABC"));
        assertTrue(HamRadioUtils.isValidCallsign("G4ABC"));
    }

    @Test
    @SuppressWarnings("deprecation") // Testing deprecated method
    void shouldValidate_Callsign_Invalid() {
        assertFalse(HamRadioUtils.isValidCallsign("ABC")); // No number
        assertFalse(HamRadioUtils.isValidCallsign("1")); // Too short
        assertFalse(HamRadioUtils.isValidCallsign("")); // Empty
        assertFalse(HamRadioUtils.isValidCallsign(null)); // Null
        assertFalse(HamRadioUtils.isValidCallsign("Q1ABC")); // Q prefix reserved for Q-codes
        // Note: W1AB1 is now valid (LAST_CHAR_NOT_LETTER check removed to support SWL callsigns)
    }

    @Test
    void shouldValidate_Coordinates_Valid() {
        assertDoesNotThrow(() -> new Coordinates(47.6062, -122.3321));
        assertDoesNotThrow(() -> new Coordinates(0, 0));
        assertDoesNotThrow(() -> new Coordinates(90, 180));
        assertDoesNotThrow(() -> new Coordinates(-90, -180));
    }

    @Test
    void shouldValidate_Coordinates_InvalidLatitude() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(91, 0));
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(-91, 0));
    }

    @Test
    void shouldValidate_Coordinates_InvalidLongitude() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(0, 181));
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(0, -181));
    }
}
