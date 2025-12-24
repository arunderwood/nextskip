package io.nextskip.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ParsingUtils.
 */
class ParsingUtilsTest {

    @Test
    void testParseTimestamp_Iso8601WithZ() {
        String timestamp = "2025-12-15T04:19:19Z";

        Instant result = ParsingUtils.parseTimestamp(timestamp, "TEST");

        assertNotNull(result);
        assertEquals(Instant.parse("2025-12-15T04:19:19Z"), result);
    }

    @Test
    void testParseTimestamp_WithoutTimezone() {
        String timestamp = "2025-12-15T04:19:19";

        Instant result = ParsingUtils.parseTimestamp(timestamp, "TEST");

        assertNotNull(result);
        // Should be parsed as UTC
        assertEquals(Instant.parse("2025-12-15T04:19:19Z"), result);
    }

    @Test
    void testParseTimestamp_Null() {
        Instant before = Instant.now();
        Instant result = ParsingUtils.parseTimestamp(null, "TEST");
        Instant after = Instant.now();

        assertNotNull(result);
        // Should return current time
        assertTrue(!result.isBefore(before) && !result.isAfter(after));
    }

    @Test
    void testParseTimestamp_Blank() {
        Instant before = Instant.now();
        Instant result = ParsingUtils.parseTimestamp("   ", "TEST");
        Instant after = Instant.now();

        assertNotNull(result);
        // Should return current time
        assertTrue(!result.isBefore(before) && !result.isAfter(after));
    }

    @Test
    void testParseTimestamp_InvalidFormat() {
        Instant before = Instant.now();
        Instant result = ParsingUtils.parseTimestamp("not-a-date", "TEST");
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
        assertNull(ParsingUtils.parseDouble("   "));
    }

    @Test
    void testParseDouble_InvalidFormat() {
        assertNull(ParsingUtils.parseDouble("not-a-number"));
    }

    @Test
    void testParseDouble_WithFieldName_ValidNumber() {
        Double result = ParsingUtils.parseDouble("14074.5", "frequency");

        assertEquals(14074.5, result, 0.001);
    }

    @Test
    void testParseDouble_WithFieldName_InvalidFormat() {
        // Should log debug message but return null
        assertNull(ParsingUtils.parseDouble("not-a-number", "frequency"));
    }

    @Test
    void testParseDouble_WithFieldName_Null() {
        assertNull(ParsingUtils.parseDouble(null, "frequency"));
    }

    @Test
    void testParseDouble_WithFieldName_Blank() {
        assertNull(ParsingUtils.parseDouble("   ", "frequency"));
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
