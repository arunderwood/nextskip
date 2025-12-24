package io.nextskip.common.util;

import io.nextskip.common.model.Coordinates;
import io.nextskip.common.model.GridSquare;

import java.util.Locale;

/**
 * Utility methods for amateur radio calculations and conversions.
 *
 * <p>Provides utilities for:
 * <ul>
 *   <li><b>Grid Squares</b>: Convert coordinates to Maidenhead grid locators</li>
 *   <li><b>Bearing</b>: Calculate azimuth between two coordinates</li>
 *   <li><b>Frequency</b>: Format and parse frequency strings (Hz, kHz, MHz)</li>
 *   <li><b>Callsigns</b>: Basic validation of amateur radio callsign format</li>
 * </ul>
 */
public final class HamRadioUtils {

    // Grid precision levels (character lengths)
    private static final int FIELD_PRECISION = 2;
    private static final int SQUARE_PRECISION = 4;
    private static final int SUBSQUARE_PRECISION = 6;
    private static final int EXTENDED_PRECISION = 8;

    // Frequency unit thresholds (in Hz)
    private static final long MHZ_THRESHOLD = 1_000_000L;
    private static final long KHZ_THRESHOLD = 1_000L;

    private HamRadioUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Convert coordinates to a 6-character Maidenhead grid square.
     *
     * @param coordinates The coordinates to convert
     * @return GridSquare representing the location
     */
    public static GridSquare coordinatesToGridSquare(Coordinates coordinates) {
        return coordinatesToGridSquare(coordinates, 6);
    }

    /**
     * Convert coordinates to a Maidenhead grid square with specified precision.
     *
     * @param coordinates The coordinates to convert
     * @param precision   Number of characters (2, 4, 6, or 8)
     * @return GridSquare representing the location
     */
    public static GridSquare coordinatesToGridSquare(Coordinates coordinates, int precision) {
        if (precision != FIELD_PRECISION && precision != SQUARE_PRECISION
                && precision != SUBSQUARE_PRECISION && precision != EXTENDED_PRECISION) {
            throw new IllegalArgumentException("Precision must be 2, 4, 6, or 8");
        }

        double lat = coordinates.latitude() + 90.0;
        double lon = coordinates.longitude() + 180.0;

        StringBuilder grid = new StringBuilder();

        // Field (2 characters)
        grid.append((char) ('A' + (int) (lon / 20.0)));
        grid.append((char) ('A' + (int) (lat / 10.0)));

        if (precision >= SQUARE_PRECISION) {
            // Square (2 digits)
            lon = lon % 20.0;
            lat = lat % 10.0;
            grid.append((int) (lon / 2.0));
            grid.append((int) (lat / 1.0));
        }

        if (precision >= SUBSQUARE_PRECISION) {
            // Subsquare (2 letters)
            lon = lon % 2.0;
            lat = lat % 1.0;
            grid.append((char) ('A' + (int) (lon * 12.0)));
            grid.append((char) ('A' + (int) (lat * 24.0)));
        }

        if (precision == EXTENDED_PRECISION) {
            // Extended subsquare (2 digits)
            lon = (lon * 12.0) % 1.0;
            lat = (lat * 24.0) % 1.0;
            grid.append((int) (lon * 10.0));
            grid.append((int) (lat * 10.0));
        }

        return new GridSquare(grid.toString());
    }

    /**
     * Calculate the bearing (azimuth) from one coordinate to another.
     *
     * @param from Starting coordinates
     * @param to   Destination coordinates
     * @return Bearing in degrees (0-360), where 0 is North
     */
    public static double bearing(Coordinates from, Coordinates to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double lon1 = Math.toRadians(from.longitude());
        double lon2 = Math.toRadians(to.longitude());

        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));

        return (bearing + 360.0) % 360.0;
    }

    /**
     * Format a frequency in Hz to a human-readable string.
     *
     * @param frequencyHz Frequency in Hz
     * @return Formatted string (e.g., "14.074 MHz", "7.200 MHz")
     */
    public static String formatFrequency(long frequencyHz) {
        if (frequencyHz >= MHZ_THRESHOLD) {
            double mhz = frequencyHz / (double) MHZ_THRESHOLD;
            return String.format(Locale.ROOT, "%.3f MHz", mhz);
        } else if (frequencyHz >= KHZ_THRESHOLD) {
            double khz = frequencyHz / (double) KHZ_THRESHOLD;
            return String.format(Locale.ROOT, "%.3f kHz", khz);
        } else {
            return frequencyHz + " Hz";
        }
    }

    /**
     * Parse a frequency string to Hz.
     * Supports formats like "14.074 MHz", "7200 kHz", "145.500MHz"
     *
     * @param frequencyStr Frequency string
     * @return Frequency in Hz, or null if parsing fails
     */
    public static Long parseFrequency(String frequencyStr) {
        if (frequencyStr == null || frequencyStr.isBlank()) {
            return null;
        }

        String cleaned = frequencyStr.trim().toUpperCase(Locale.ROOT);

        try {
            if (cleaned.contains("MHZ") || cleaned.contains("M")) {
                String numStr = cleaned.replaceAll("[^0-9.]", "");
                double mhz = Double.parseDouble(numStr);
                return (long) (mhz * MHZ_THRESHOLD);
            } else if (cleaned.contains("KHZ") || cleaned.contains("K")) {
                String numStr = cleaned.replaceAll("[^0-9.]", "");
                double khz = Double.parseDouble(numStr);
                return (long) (khz * KHZ_THRESHOLD);
            } else if (cleaned.contains("HZ")) {
                String numStr = cleaned.replaceAll("[^0-9.]", "");
                return (long) Double.parseDouble(numStr);
            } else {
                // Assume Hz if no unit specified
                return Long.parseLong(cleaned);
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validate a callsign format (basic validation).
     *
     * @param callsign The callsign to validate
     * @return true if the callsign appears valid
     */
    public static boolean isValidCallsign(String callsign) {
        if (callsign == null || callsign.isBlank()) {
            return false;
        }

        // Basic regex for amateur radio callsigns
        // Most callsigns are 3-6 characters with at least one number
        String pattern = "^[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,3}$";
        return callsign.toUpperCase(Locale.ROOT).matches(pattern);
    }
}
