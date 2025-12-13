package io.nextskip.common.util;

import io.nextskip.common.model.Coordinates;
import io.nextskip.common.model.GridSquare;

/**
 * Utility methods for amateur radio calculations and conversions.
 */
public final class HamRadioUtils {

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
        if (precision != 2 && precision != 4 && precision != 6 && precision != 8) {
            throw new IllegalArgumentException("Precision must be 2, 4, 6, or 8");
        }

        double lat = coordinates.latitude() + 90.0;
        double lon = coordinates.longitude() + 180.0;

        StringBuilder grid = new StringBuilder();

        // Field (2 characters)
        grid.append((char) ('A' + (int) (lon / 20.0)));
        grid.append((char) ('A' + (int) (lat / 10.0)));

        if (precision >= 4) {
            // Square (2 digits)
            lon = lon % 20.0;
            lat = lat % 10.0;
            grid.append((int) (lon / 2.0));
            grid.append((int) (lat / 1.0));
        }

        if (precision >= 6) {
            // Subsquare (2 letters)
            lon = lon % 2.0;
            lat = lat % 1.0;
            grid.append((char) ('A' + (int) (lon * 12.0)));
            grid.append((char) ('A' + (int) (lat * 24.0)));
        }

        if (precision == 8) {
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
        if (frequencyHz >= 1_000_000) {
            double mhz = frequencyHz / 1_000_000.0;
            return String.format("%.3f MHz", mhz);
        } else if (frequencyHz >= 1_000) {
            double khz = frequencyHz / 1_000.0;
            return String.format("%.3f kHz", khz);
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

        String cleaned = frequencyStr.trim().toUpperCase();

        try {
            if (cleaned.contains("MHZ") || cleaned.contains("M")) {
                String numStr = cleaned.replaceAll("[^0-9.]", "");
                double mhz = Double.parseDouble(numStr);
                return (long) (mhz * 1_000_000);
            } else if (cleaned.contains("KHZ") || cleaned.contains("K")) {
                String numStr = cleaned.replaceAll("[^0-9.]", "");
                double khz = Double.parseDouble(numStr);
                return (long) (khz * 1_000);
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
        return callsign.toUpperCase().matches(pattern);
    }

    /**
     * Calculate sunrise/sunset times (simplified calculation).
     * Note: This is a simplified calculation. For production use,
     * consider using a library like SunCalc or similar.
     *
     * @param coordinates Location coordinates
     * @param julianDay   Julian day number
     * @return Array of [sunrise hour, sunset hour] in UTC (0-24)
     */
    public static double[] calculateSunriseSunset(Coordinates coordinates, double julianDay) {
        // Simplified calculation - not astronomically precise
        // For a production system, use a proper astronomical calculation library

        double lat = Math.toRadians(coordinates.latitude());
        double n = julianDay - 2451545.0;
        double l = (280.460 + 0.9856474 * n) % 360.0;
        double g = (357.528 + 0.9856003 * n) % 360.0;
        double lambda = l + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g));
        double epsilon = 23.439 - 0.0000004 * n;
        double alpha = Math.toDegrees(Math.atan2(
                Math.cos(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda)),
                Math.cos(Math.toRadians(lambda))));
        double delta = Math.asin(Math.sin(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda)));

        double cosH = -Math.tan(lat) * Math.tan(delta);
        if (cosH > 1.0) {
            // Polar night
            return new double[]{0.0, 0.0};
        } else if (cosH < -1.0) {
            // Polar day
            return new double[]{0.0, 24.0};
        }

        double h = Math.toDegrees(Math.acos(cosH));
        double sunrise = 12.0 - h / 15.0;
        double sunset = 12.0 + h / 15.0;

        return new double[]{sunrise, sunset};
    }
}
