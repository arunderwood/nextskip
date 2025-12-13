package io.nextskip.common.model;

/**
 * Maidenhead Grid Square Locator System representation.
 *
 * The Maidenhead locator system is a geographic coordinate system
 * used by amateur radio operators. It compresses latitude/longitude
 * into a short alphanumeric string (e.g., "CN87" or "FN31pr").
 *
 * @param value The grid square string (e.g., "CN87", "FN31pr")
 */
public record GridSquare(String value) {

    public GridSquare {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Grid square cannot be null or blank");
        }
        // Basic validation: must be 2, 4, 6, or 8 characters
        int len = value.length();
        if (len != 2 && len != 4 && len != 6 && len != 8) {
            throw new IllegalArgumentException(
                    "Grid square must be 2, 4, 6, or 8 characters long, got: " + len);
        }
        // Normalize to uppercase for consistency
        value = value.toUpperCase();
    }

    /**
     * Convert grid square to approximate center coordinates.
     *
     * @return Coordinates representing the center of the grid square
     */
    public Coordinates toCoordinates() {
        String grid = value.toUpperCase();
        int len = grid.length();

        // Field (first 2 characters) - 20 degrees longitude, 10 degrees latitude
        double lon = (grid.charAt(0) - 'A') * 20.0 - 180.0;
        double lat = (grid.charAt(1) - 'A') * 10.0 - 90.0;

        if (len >= 4) {
            // Square (next 2 digits) - 2 degrees longitude, 1 degree latitude
            lon += (grid.charAt(2) - '0') * 2.0;
            lat += (grid.charAt(3) - '0') * 1.0;
        }

        if (len >= 6) {
            // Subsquare (next 2 letters) - 5 minutes longitude, 2.5 minutes latitude
            lon += (grid.charAt(4) - 'A') * (2.0 / 24.0);
            lat += (grid.charAt(5) - 'A') * (1.0 / 24.0);
        }

        if (len == 8) {
            // Extended subsquare (next 2 digits)
            lon += (grid.charAt(6) - '0') * (2.0 / 240.0);
            lat += (grid.charAt(7) - '0') * (1.0 / 240.0);
        }

        // Add half of the grid square size to get center point
        switch (len) {
            case 2 -> {
                lon += 10.0;
                lat += 5.0;
            }
            case 4 -> {
                lon += 1.0;
                lat += 0.5;
            }
            case 6 -> {
                lon += 2.0 / 48.0;
                lat += 1.0 / 48.0;
            }
            case 8 -> {
                lon += 2.0 / 480.0;
                lat += 1.0 / 480.0;
            }
            default -> {
                // Default to 4-character precision
            }
        }

        return new Coordinates(lat, lon);
    }

    /**
     * Get the precision of this grid square in kilometers.
     *
     * @return Approximate precision in kilometers
     */
    public double precisionKm() {
        return switch (value.length()) {
            case 2 -> 1000.0;  // Field level: ~1000km
            case 4 -> 100.0;   // Square level: ~100km
            case 6 -> 5.0;     // Subsquare level: ~5km
            case 8 -> 0.5;     // Extended subsquare: ~500m
            default -> 0.0;
        };
    }
}
