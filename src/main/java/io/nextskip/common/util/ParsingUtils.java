package io.nextskip.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared parsing utilities for external API data.
 *
 * <p>Provides flexible parsing methods that handle common format variations
 * from external APIs like POTA, SOTA, and other ham radio data sources.
 */
public final class ParsingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ParsingUtils.class);

    /**
     * ISO-8601 format without timezone suffix (common in ham radio APIs).
     */
    private static final DateTimeFormatter NO_TIMEZONE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private ParsingUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Parse a timestamp string with flexible format handling.
     *
     * <p>Supports:
     * <ul>
     *   <li>ISO-8601 with 'Z' suffix (e.g., "2025-12-15T04:19:19Z")</li>
     *   <li>ISO-8601 without timezone (e.g., "2025-12-15T04:19:19") - assumes UTC</li>
     * </ul>
     *
     * @param timestamp the timestamp string to parse
     * @param sourceName source name for logging (e.g., "POTA", "SOTA")
     * @return parsed Instant, or current time if null/blank/unparseable
     */
    public static Instant parseTimestamp(String timestamp, String sourceName) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }

        try {
            // Try parsing with 'Z' suffix first (ISO-8601)
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e1) {
            try {
                // Try parsing without timezone (assume UTC)
                LocalDateTime dateTime = LocalDateTime.parse(timestamp, NO_TIMEZONE_FORMATTER);
                return dateTime.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unable to parse timestamp from {}: '{}', using current time",
                            sourceName, timestamp);
                }
                return Instant.now();
            }
        }
    }

    /**
     * Safely parse a string to Double.
     *
     * @param value the string value to parse
     * @return parsed Double, or null if null/blank/invalid format
     */
    public static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safely parse a string to Double with logging for invalid values.
     *
     * @param value the string value to parse
     * @param fieldName field name for logging (e.g., "frequency", "latitude")
     * @return parsed Double, or null if null/blank/invalid format
     */
    public static Double parseDouble(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid {} format: {}", fieldName, value);
            }
            return null;
        }
    }

    /**
     * Parse frequency in MHz and convert to kHz.
     *
     * <p>Used by APIs that report frequency in MHz (like SOTA) when
     * internal representation uses kHz.
     *
     * @param frequencyMHz frequency string in MHz
     * @return frequency in kHz, or null if invalid
     */
    public static Double parseFrequencyMhzToKhz(String frequencyMHz) {
        Double freq = parseDouble(frequencyMHz, "frequency");
        return freq != null ? freq * 1000.0 : null;
    }

    /**
     * Parse region code from a location descriptor.
     *
     * <p>Format: "{country}-{region}" (e.g., "US-CO" → "CO", "JP-ST" → "ST")
     *
     * @param locationDesc location descriptor (e.g., "US-CO")
     * @return region code after the hyphen, or null if not parseable
     */
    public static String parseRegionCode(String locationDesc) {
        if (locationDesc == null || locationDesc.isBlank()) {
            return null;
        }

        int hyphenIndex = locationDesc.indexOf('-');
        if (hyphenIndex > 0 && hyphenIndex < locationDesc.length() - 1) {
            return locationDesc.substring(hyphenIndex + 1);
        }

        return null;
    }

    /**
     * Parse country code from a location descriptor.
     *
     * <p>Format: "{country}-{region}" (e.g., "US-CO" → "US", "JP-ST" → "JP")
     *
     * @param locationDesc location descriptor (e.g., "US-CO")
     * @return country code before the hyphen, or null if not parseable
     */
    public static String parseCountryCode(String locationDesc) {
        if (locationDesc == null || locationDesc.isBlank()) {
            return null;
        }

        int hyphenIndex = locationDesc.indexOf('-');
        if (hyphenIndex > 0) {
            return locationDesc.substring(0, hyphenIndex);
        }

        return null;
    }
}
