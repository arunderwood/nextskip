package io.nextskip.common.model;

/*
 * Implementation Note: Custom Maidenhead Grid Square Implementation
 *
 * The algorithm follows the IARU Maidenhead Locator System specification
 * (adopted 1980, clarified 1999 with WGS-84 reference).
 * See GridSquareTest.java for comprehensive verification.
 */

/**
 * Maidenhead Grid Square Locator System representation.
 *
 * <p>The Maidenhead locator system is a geographic coordinate system
 * used by amateur radio operators. It compresses latitude/longitude
 * into a short alphanumeric string (e.g., "CN87" or "FN31pr").
 *
 * <h2>System Structure</h2>
 * <ul>
 *   <li><b>Field (2 chars):</b> Letters A-R, 20° lon × 10° lat (~1000 km)</li>
 *   <li><b>Square (2 chars):</b> Digits 0-9, 2° lon × 1° lat (~100 km)</li>
 *   <li><b>Subsquare (2 chars):</b> Letters A-X, 5' lon × 2.5' lat (~5 km)</li>
 *   <li><b>Extended (2 chars):</b> Digits 0-9, 30" lon × 15" lat (~500 m)</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre>
 * GridSquare field = new GridSquare("CN");           // Seattle area, field
 * GridSquare square = new GridSquare("CN87");        // Seattle area, square
 * GridSquare subsquare = new GridSquare("CN87ts");   // Seattle downtown
 * GridSquare extended = new GridSquare("CN87ts50");  // Precise location
 * </pre>
 *
 * <p><b>Reference:</b> IARU Maidenhead Locator System (1980), WGS-84 (1999)
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
     * <p>Returns the geographic center point of the grid square. Precision
     * depends on grid square length:
     * <ul>
     *   <li>2 chars: Center of ~1000 km × 1000 km field</li>
     *   <li>4 chars: Center of ~100 km × 100 km square</li>
     *   <li>6 chars: Center of ~5 km × 5 km subsquare</li>
     *   <li>8 chars: Center of ~500 m × 500 m extended square</li>
     * </ul>
     *
     * <p><b>Algorithm:</b> Decodes each precision level by calculating offsets
     * from the origin (-180°, -90°) then adds half the grid square dimensions
     * to obtain the center point.
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
