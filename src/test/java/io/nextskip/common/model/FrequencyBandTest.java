package io.nextskip.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite for FrequencyBand enum.
 *
 * Tests the frequency band lookup, parsing, and containment logic
 * for amateur radio HF and VHF bands.
 *
 * Test categories:
 * 1. getCenterKhz() - Center frequency calculation (1 test)
 * 2. contains() - Frequency containment logic (3 tests)
 * 3. containsHz() - Hz to kHz conversion and containment (2 tests)
 * 4. fromFrequencyKhz() - Band lookup by frequency (3 tests)
 * 5. fromFrequencyHz() - Band lookup by Hz frequency (1 test)
 * 6. fromString() - String parsing (4 tests)
 */
class FrequencyBandTest {

    // ==========================================================================
    // Category 1: getCenterKhz() - Center Frequency Calculation
    // ==========================================================================

    @Test
    void testGetCenterKhz_AllBands() {
        // Verify center calculation for each band
        assertEquals(1900, FrequencyBand.BAND_160M.getCenterKhz());  // (1800 + 2000) / 2
        assertEquals(3750, FrequencyBand.BAND_80M.getCenterKhz());   // (3500 + 4000) / 2
        assertEquals(5367, FrequencyBand.BAND_60M.getCenterKhz());   // (5330 + 5405) / 2
        assertEquals(7150, FrequencyBand.BAND_40M.getCenterKhz());   // (7000 + 7300) / 2
        assertEquals(10125, FrequencyBand.BAND_30M.getCenterKhz());  // (10100 + 10150) / 2
        assertEquals(14175, FrequencyBand.BAND_20M.getCenterKhz());  // (14000 + 14350) / 2
        assertEquals(18118, FrequencyBand.BAND_17M.getCenterKhz());  // (18068 + 18168) / 2
        assertEquals(21225, FrequencyBand.BAND_15M.getCenterKhz());  // (21000 + 21450) / 2
        assertEquals(24940, FrequencyBand.BAND_12M.getCenterKhz());  // (24890 + 24990) / 2
        assertEquals(28850, FrequencyBand.BAND_10M.getCenterKhz());  // (28000 + 29700) / 2
        assertEquals(52000, FrequencyBand.BAND_6M.getCenterKhz());   // (50000 + 54000) / 2
        assertEquals(146000, FrequencyBand.BAND_2M.getCenterKhz());  // (144000 + 148000) / 2
    }

    // ==========================================================================
    // Category 2: contains() - Frequency Containment Logic
    // ==========================================================================

    @Test
    void testContains_FrequencyInBand() {
        // Frequency clearly within band
        assertTrue(FrequencyBand.BAND_20M.contains(14100));
        assertTrue(FrequencyBand.BAND_40M.contains(7100));
        assertTrue(FrequencyBand.BAND_80M.contains(3700));
        assertTrue(FrequencyBand.BAND_160M.contains(1900));
    }

    @Test
    void testContains_FrequencyOutOfBand() {
        // Frequency clearly outside band
        assertFalse(FrequencyBand.BAND_20M.contains(14400));  // Above 20m range
        assertFalse(FrequencyBand.BAND_20M.contains(13900));  // Below 20m range
        assertFalse(FrequencyBand.BAND_40M.contains(5000));   // Between bands
        assertFalse(FrequencyBand.BAND_80M.contains(3000));   // Below 80m range
    }

    @Test
    void testContains_BoundaryValues() {
        // Test exact start and end frequencies (inclusive boundaries)
        assertTrue(FrequencyBand.BAND_20M.contains(14000));  // Start (inclusive)
        assertTrue(FrequencyBand.BAND_20M.contains(14350));  // End (inclusive)
        assertTrue(FrequencyBand.BAND_40M.contains(7000));   // Start (inclusive)
        assertTrue(FrequencyBand.BAND_40M.contains(7300));   // End (inclusive)

        // Just outside boundaries
        assertFalse(FrequencyBand.BAND_20M.contains(13999)); // Below start
        assertFalse(FrequencyBand.BAND_20M.contains(14351)); // Above end
    }

    // ==========================================================================
    // Category 3: containsHz() - Hz to kHz Conversion and Containment
    // ==========================================================================

    @Test
    void testContainsHz_ValidFrequency() {
        // Valid Hz frequencies within bands
        assertTrue(FrequencyBand.BAND_20M.containsHz(14100000L));  // 14100 kHz
        assertTrue(FrequencyBand.BAND_40M.containsHz(7100000L));   // 7100 kHz
        assertTrue(FrequencyBand.BAND_2M.containsHz(146000000L));  // 146 MHz (146000 kHz)

        // Hz frequencies outside bands
        assertFalse(FrequencyBand.BAND_20M.containsHz(14400000L)); // 14400 kHz (above range)
        assertFalse(FrequencyBand.BAND_40M.containsHz(5000000L));  // 5000 kHz (below range)
    }

    @Test
    void testContainsHz_BoundaryConversion() {
        // Test Hz to kHz conversion at boundaries
        assertTrue(FrequencyBand.BAND_20M.containsHz(14000000L));  // Exact start in Hz
        assertTrue(FrequencyBand.BAND_20M.containsHz(14350000L));  // Exact end in Hz

        // Just outside boundaries in Hz
        assertFalse(FrequencyBand.BAND_20M.containsHz(13999000L)); // Below start
        assertFalse(FrequencyBand.BAND_20M.containsHz(14351000L)); // Above end
    }

    // ==========================================================================
    // Category 4: fromFrequencyKhz() - Band Lookup by Frequency
    // ==========================================================================

    @Test
    void testFromFrequencyKhz_ValidMatch() {
        // Frequencies that should match specific bands
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromFrequencyKhz(7100));
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromFrequencyKhz(14100));
        assertEquals(FrequencyBand.BAND_80M, FrequencyBand.fromFrequencyKhz(3700));
        assertEquals(FrequencyBand.BAND_15M, FrequencyBand.fromFrequencyKhz(21200));
        assertEquals(FrequencyBand.BAND_2M, FrequencyBand.fromFrequencyKhz(146000));
    }

    @Test
    void testFromFrequencyKhz_NoMatch() {
        // Frequencies outside all bands
        assertNull(FrequencyBand.fromFrequencyKhz(5000));   // Between bands
        assertNull(FrequencyBand.fromFrequencyKhz(1000));   // Below all bands
        assertNull(FrequencyBand.fromFrequencyKhz(150000)); // Above all bands
        assertNull(FrequencyBand.fromFrequencyKhz(13000));  // Between 17m and 20m
    }

    @Test
    void testFromFrequencyKhz_BoundaryMatch() {
        // Exact boundary frequencies should match (inclusive)
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromFrequencyKhz(7000));  // Start
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromFrequencyKhz(7300));  // End
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromFrequencyKhz(14000)); // Start
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromFrequencyKhz(14350)); // End
    }

    // ==========================================================================
    // Category 5: fromFrequencyHz() - Band Lookup by Hz Frequency
    // ==========================================================================

    @Test
    void testFromFrequencyHz_Delegation() {
        // Verify Hz to kHz conversion and delegation to fromFrequencyKhz
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromFrequencyHz(7100000L));
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromFrequencyHz(14100000L));
        assertEquals(FrequencyBand.BAND_2M, FrequencyBand.fromFrequencyHz(146000000L));

        // Frequencies outside all bands (in Hz)
        assertNull(FrequencyBand.fromFrequencyHz(5000000L));   // 5000 kHz
        assertNull(FrequencyBand.fromFrequencyHz(150000000L)); // 150 MHz
    }

    // ==========================================================================
    // Category 6: fromString() - String Parsing
    // ==========================================================================

    @Test
    void testFromString_ValidBands() {
        // Valid band name strings
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromString("20m"));
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromString("40m"));
        assertEquals(FrequencyBand.BAND_80M, FrequencyBand.fromString("80m"));
        assertEquals(FrequencyBand.BAND_160M, FrequencyBand.fromString("160m"));
        assertEquals(FrequencyBand.BAND_6M, FrequencyBand.fromString("6m"));
        assertEquals(FrequencyBand.BAND_2M, FrequencyBand.fromString("2m"));
    }

    @Test
    void testFromString_CaseInsensitive() {
        // Band name parsing should be case-insensitive
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromString("20m"));
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromString("20M"));
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromString("40m"));
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromString("40M"));

        // With whitespace
        assertEquals(FrequencyBand.BAND_20M, FrequencyBand.fromString(" 20m "));
        assertEquals(FrequencyBand.BAND_40M, FrequencyBand.fromString(" 40M "));
    }

    @Test
    void testFromString_Null() {
        // Null input should return null
        assertNull(FrequencyBand.fromString(null));
    }

    @Test
    void testFromString_Blank() {
        // Blank/empty input should return null
        assertNull(FrequencyBand.fromString(""));
        assertNull(FrequencyBand.fromString("   "));
        assertNull(FrequencyBand.fromString("\t"));
    }
}
