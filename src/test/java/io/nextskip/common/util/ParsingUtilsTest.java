package io.nextskip.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ParsingUtils.
 */
@SuppressWarnings("PMD.TooManyMethods") // Utility class requires many test methods for complete coverage
class ParsingUtilsTest {

    private static final String TEST_SOURCE = "TEST";
    private static final String ISO_TIMESTAMP = "2025-12-15T04:19:19Z";
    private static final String FREQUENCY_FIELD = "frequency";
    private static final String BLANK_STRING = "   ";

    @Test
    void testParseTimestamp_Iso8601WithZ() {
        String timestamp = ISO_TIMESTAMP;

        Instant result = ParsingUtils.parseTimestamp(timestamp, TEST_SOURCE);

        assertNotNull(result);
        assertEquals(Instant.parse(ISO_TIMESTAMP), result);
    }

    @Test
    void testParseTimestamp_WithoutTimezone() {
        String timestamp = "2025-12-15T04:19:19";

        Instant result = ParsingUtils.parseTimestamp(timestamp, TEST_SOURCE);

        assertNotNull(result);
        // Should be parsed as UTC
        assertEquals(Instant.parse(ISO_TIMESTAMP), result);
    }

    @Test
    void testParseTimestamp_Null() {
        Instant before = Instant.now();
        Instant result = ParsingUtils.parseTimestamp(null, TEST_SOURCE);
        Instant after = Instant.now();

        assertNotNull(result);
        // Should return current time
        assertTrue(!result.isBefore(before) && !result.isAfter(after));
    }

    @Test
    void testParseTimestamp_Blank() {
        Instant before = Instant.now();
        Instant result = ParsingUtils.parseTimestamp(BLANK_STRING, TEST_SOURCE);
        Instant after = Instant.now();

        assertNotNull(result);
        // Should return current time
        assertTrue(!result.isBefore(before) && !result.isAfter(after));
    }

    @Test
    void testParseTimestamp_InvalidFormat() {
        Instant before = Instant.now();
        Instant result = ParsingUtils.parseTimestamp("not-a-date", TEST_SOURCE);
        Instant after = Instant.now();

        assertNotNull(result);
        // Should return current time
        assertTrue(!result.isBefore(before) && !result.isAfter(after));
    }

    @Test
    void testParseDouble_ValidNumber() {
        Double result = ParsingUtils.parseDouble("14074.5");

        assertEquals(14074.5, result, 0.001);
    }

    @Test
    void testParseDouble_Null() {
        assertNull(ParsingUtils.parseDouble(null));
    }

    @Test
    void testParseDouble_Blank() {
        assertNull(ParsingUtils.parseDouble(BLANK_STRING));
    }

    @Test
    void testParseDouble_InvalidFormat() {
        assertNull(ParsingUtils.parseDouble("not-a-number"));
    }

    @Test
    void testParseDouble_WithFieldName_ValidNumber() {
        Double result = ParsingUtils.parseDouble("14074.5", FREQUENCY_FIELD);

        assertEquals(14074.5, result, 0.001);
    }

    @Test
    void testParseDouble_WithFieldName_InvalidFormat() {
        // Should log debug message but return null
        assertNull(ParsingUtils.parseDouble("not-a-number", FREQUENCY_FIELD));
    }

    @Test
    void testParseDouble_WithFieldName_Null() {
        assertNull(ParsingUtils.parseDouble(null, FREQUENCY_FIELD));
    }

    @Test
    void testParseDouble_WithFieldName_Blank() {
        assertNull(ParsingUtils.parseDouble(BLANK_STRING, FREQUENCY_FIELD));
    }

    @Test
    void testParseFrequencyMhzToKhz_ValidFrequency() {
        Double result = ParsingUtils.parseFrequencyMhzToKhz("14.074");

        assertEquals(14074.0, result, 0.001);
    }

    @Test
    void testParseFrequencyMhzToKhz_Null() {
        assertNull(ParsingUtils.parseFrequencyMhzToKhz(null));
    }

    @Test
    void testParseFrequencyMhzToKhz_InvalidFormat() {
        assertNull(ParsingUtils.parseFrequencyMhzToKhz("invalid"));
    }

    @Test
    void testParseRegionCode_ValidLocation() {
        assertEquals("CO", ParsingUtils.parseRegionCode("US-CO"));
        assertEquals("ST", ParsingUtils.parseRegionCode("JP-ST"));
        assertEquals("ON", ParsingUtils.parseRegionCode("VE-ON"));
    }

    @Test
    void testParseRegionCode_Null() {
        assertNull(ParsingUtils.parseRegionCode(null));
    }

    @Test
    void testParseRegionCode_Blank() {
        assertNull(ParsingUtils.parseRegionCode("   "));
    }

    @Test
    void testParseRegionCode_NoHyphen() {
        assertNull(ParsingUtils.parseRegionCode("USCO"));
    }

    @Test
    void testParseRegionCode_HyphenAtStart() {
        assertNull(ParsingUtils.parseRegionCode("-CO"));
    }

    @Test
    void testParseRegionCode_HyphenAtEnd() {
        assertNull(ParsingUtils.parseRegionCode("US-"));
    }

    @Test
    void testParseCountryCode_ValidLocation() {
        assertEquals("US", ParsingUtils.parseCountryCode("US-CO"));
        assertEquals("JP", ParsingUtils.parseCountryCode("JP-ST"));
        assertEquals("VE", ParsingUtils.parseCountryCode("VE-ON"));
    }

    @Test
    void testParseCountryCode_Null() {
        assertNull(ParsingUtils.parseCountryCode(null));
    }

    @Test
    void testParseCountryCode_Blank() {
        assertNull(ParsingUtils.parseCountryCode("   "));
    }

    @Test
    void testParseCountryCode_NoHyphen() {
        assertNull(ParsingUtils.parseCountryCode("USCO"));
    }

    @Test
    void testParseCountryCode_HyphenAtStart() {
        // Returns empty string (substring from 0 to 0)
        assertNull(ParsingUtils.parseCountryCode("-CO"));
    }
}
