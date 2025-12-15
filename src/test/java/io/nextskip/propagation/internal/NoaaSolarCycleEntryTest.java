package io.nextskip.propagation.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite for NoaaSolarCycleEntry record.
 *
 * Tests the validation logic for NOAA Space Weather API data:
 * - solarFlux validation (null check, range 0-1000)
 * - sunspotNumber validation (null check, range 0-1000)
 * - timeTag validation (null/blank check)
 *
 * Test categories:
 * 1. Valid data - no exceptions (1 test)
 * 2. solarFlux validation (4 tests)
 * 3. sunspotNumber validation (4 tests)
 * 4. timeTag validation (2 tests)
 */
class NoaaSolarCycleEntryTest {

    // ==========================================================================
    // Category 1: Valid Data - No Exceptions
    // ==========================================================================

    @Test
    void testValidate_ValidEntry_NoException() {
        // Valid data should not throw any exceptions
        var entry = new NoaaSolarCycleEntry("2025-01", 150.0, 100);
        assertDoesNotThrow(entry::validate);

        var entry2 = new NoaaSolarCycleEntry("2025-12-15", 200.5, 50);
        assertDoesNotThrow(entry2::validate);

        // Partial date format (YYYY-MM) should be valid
        var partialDate = new NoaaSolarCycleEntry("2025-11", 100.0, 75);
        assertDoesNotThrow(partialDate::validate);
    }

    // ==========================================================================
    // Category 2: solarFlux Validation
    // ==========================================================================

    @Test
    void testValidate_NullSolarFlux_ThrowsException() {
        // Null solar flux should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry("2025-01", null, 100);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Missing required field: f10.7 (solar flux)"));
    }

    @Test
    void testValidate_NegativeSolarFlux_ThrowsException() {
        // Negative solar flux should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry("2025-01", -0.1, 100);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Solar flux out of expected range [0, 1000]"));
        assertTrue(exception.getMessage().contains("-0.1"));
    }

    @Test
    void testValidate_SolarFluxTooHigh_ThrowsException() {
        // Solar flux above 1000 should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry("2025-01", 1000.1, 100);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Solar flux out of expected range [0, 1000]"));
        assertTrue(exception.getMessage().contains("1000.1"));
    }

    @Test
    void testValidate_SolarFluxAtBoundary() {
        // Solar flux at 0 and 1000 (boundaries) should be valid
        var entryZero = new NoaaSolarCycleEntry("2025-01", 0.0, 100);
        assertDoesNotThrow(entryZero::validate);

        var entry1000 = new NoaaSolarCycleEntry("2025-01", 1000.0, 100);
        assertDoesNotThrow(entry1000::validate);

        // Just inside boundaries
        var entryJustAboveZero = new NoaaSolarCycleEntry("2025-01", 0.001, 100);
        assertDoesNotThrow(entryJustAboveZero::validate);

        var entryJustBelow1000 = new NoaaSolarCycleEntry("2025-01", 999.999, 100);
        assertDoesNotThrow(entryJustBelow1000::validate);
    }

    // ==========================================================================
    // Category 3: sunspotNumber Validation
    // ==========================================================================

    @Test
    void testValidate_NullSunspotNumber_ThrowsException() {
        // Null sunspot number should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry("2025-01", 150.0, null);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Missing required field: ssn (sunspot number)"));
    }

    @Test
    void testValidate_NegativeSunspotNumber_ThrowsException() {
        // Negative sunspot number should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry("2025-01", 150.0, -1);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Sunspot number out of expected range [0, 1000]"));
        assertTrue(exception.getMessage().contains("-1"));
    }

    @Test
    void testValidate_SunspotNumberTooHigh_ThrowsException() {
        // Sunspot number above 1000 should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry("2025-01", 150.0, 1001);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Sunspot number out of expected range [0, 1000]"));
        assertTrue(exception.getMessage().contains("1001"));
    }

    @Test
    void testValidate_SunspotNumberAtBoundary() {
        // Sunspot number at 0 and 1000 (boundaries) should be valid
        var entryZero = new NoaaSolarCycleEntry("2025-01", 150.0, 0);
        assertDoesNotThrow(entryZero::validate);

        var entry1000 = new NoaaSolarCycleEntry("2025-01", 150.0, 1000);
        assertDoesNotThrow(entry1000::validate);

        // Just inside boundaries
        var entryOne = new NoaaSolarCycleEntry("2025-01", 150.0, 1);
        assertDoesNotThrow(entryOne::validate);

        var entry999 = new NoaaSolarCycleEntry("2025-01", 150.0, 999);
        assertDoesNotThrow(entry999::validate);
    }

    // ==========================================================================
    // Category 4: timeTag Validation
    // ==========================================================================

    @Test
    void testValidate_NullTimeTag_ThrowsException() {
        // Null time tag should throw InvalidApiResponseException
        var entry = new NoaaSolarCycleEntry(null, 150.0, 100);
        var exception = assertThrows(InvalidApiResponseException.class, entry::validate);
        assertTrue(exception.getMessage().contains("Missing required field: time-tag"));
    }

    @Test
    void testValidate_BlankTimeTag_ThrowsException() {
        // Blank time tag should throw InvalidApiResponseException
        var entryEmpty = new NoaaSolarCycleEntry("", 150.0, 100);
        var exception1 = assertThrows(InvalidApiResponseException.class, entryEmpty::validate);
        assertTrue(exception1.getMessage().contains("Missing required field: time-tag"));

        var entrySpaces = new NoaaSolarCycleEntry("   ", 150.0, 100);
        var exception2 = assertThrows(InvalidApiResponseException.class, entrySpaces::validate);
        assertTrue(exception2.getMessage().contains("Missing required field: time-tag"));

        var entryTab = new NoaaSolarCycleEntry("\t", 150.0, 100);
        var exception3 = assertThrows(InvalidApiResponseException.class, entryTab::validate);
        assertTrue(exception3.getMessage().contains("Missing required field: time-tag"));
    }
}
