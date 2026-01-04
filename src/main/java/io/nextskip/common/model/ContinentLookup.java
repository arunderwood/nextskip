package io.nextskip.common.model;

import java.util.Locale;
import java.util.Map;

/**
 * Utility for determining continent codes from grid squares and callsigns.
 *
 * <p>Provides two lookup methods:
 * <ul>
 *   <li>{@link #fromGridSquare(String)} - Uses Maidenhead field (first letter) for ~80% accuracy</li>
 *   <li>{@link #fromCallsign(Callsign)} - Placeholder for future cty.dat-based lookup</li>
 * </ul>
 *
 * <p>Continent codes follow the standard 2-letter abbreviations:
 * <ul>
 *   <li>AF - Africa</li>
 *   <li>AN - Antarctica</li>
 *   <li>AS - Asia</li>
 *   <li>EU - Europe</li>
 *   <li>NA - North America</li>
 *   <li>OC - Oceania</li>
 *   <li>SA - South America</li>
 * </ul>
 *
 * <p>Phase 2 will add accurate cty.dat-based lookup for callsign prefixes.
 */
public final class ContinentLookup {

    /**
     * Mapping of Maidenhead field (first letter A-R) to continent.
     *
     * <p>This is an approximation based on geographic regions.
     * The Maidenhead grid divides Earth into 18 fields (A-R) longitude-wise.
     * Each field spans 20 degrees of longitude.
     *
     * <p>Accuracy is approximately 80% due to:
     * <ul>
     *   <li>Continental boundaries not aligning with grid boundaries</li>
     *   <li>Islands in oceanic regions</li>
     *   <li>Multi-continent fields (e.g., Africa/Europe overlap)</li>
     * </ul>
     */
    private static final Map<String, String> GRID_FIELD_TO_CONTINENT = Map.ofEntries(
            // Field A: South Atlantic, parts of Antarctica
            Map.entry("A", "SA"),
            // Field B: South Atlantic, Antarctica
            Map.entry("B", "AN"),
            // Field C: South America (Argentina, Chile)
            Map.entry("C", "SA"),
            // Field D: South America (Brazil, Uruguay)
            Map.entry("D", "SA"),
            // Field E: North America (Caribbean, Central America)
            Map.entry("E", "NA"),
            // Field F: North America (Eastern US, Canada)
            Map.entry("F", "NA"),
            // Field G: South America (Brazil east coast)
            Map.entry("G", "SA"),
            // Field H: Africa (West Africa)
            Map.entry("H", "AF"),
            // Field I: Europe/Africa overlap - default to Europe
            Map.entry("I", "EU"),
            // Field J: Europe (UK, Western Europe)
            Map.entry("J", "EU"),
            // Field K: Europe/Africa (Mediterranean)
            Map.entry("K", "EU"),
            // Field L: Europe (Eastern Europe)
            Map.entry("L", "EU"),
            // Field M: Asia (Middle East, Russia)
            Map.entry("M", "AS"),
            // Field N: Asia (Central Asia)
            Map.entry("N", "AS"),
            // Field O: Asia (Southeast Asia)
            Map.entry("O", "AS"),
            // Field P: Asia (Japan, Korea, Eastern Russia)
            Map.entry("P", "AS"),
            // Field Q: Oceania (Australia, Pacific)
            Map.entry("Q", "OC"),
            // Field R: Oceania/Pacific
            Map.entry("R", "OC")
    );

    private ContinentLookup() {
        // Utility class
    }

    /**
     * Determines continent from a Maidenhead grid square.
     *
     * <p>Uses the first letter (field) of the grid square to approximate
     * the continent. Accuracy is approximately 80%.
     *
     * <p>Examples:
     * <pre>
     * fromGridSquare("FN31")  // "NA" (Eastern US)
     * fromGridSquare("JO01")  // "EU" (UK/Europe)
     * fromGridSquare("PM95")  // "AS" (Japan)
     * fromGridSquare("QF22")  // "OC" (Australia)
     * </pre>
     *
     * @param gridSquare the Maidenhead grid (e.g., "FN31", "JO01pr")
     * @return continent code (e.g., "NA", "EU"), or null if invalid grid
     */
    public static String fromGridSquare(String gridSquare) {
        if (gridSquare == null || gridSquare.isEmpty()) {
            return null;
        }

        String field = gridSquare.substring(0, 1).toUpperCase(Locale.ROOT);
        return GRID_FIELD_TO_CONTINENT.get(field);
    }

    /**
     * Determines continent from a callsign.
     *
     * <p><b>Note:</b> This is a placeholder for Phase 2 cty.dat integration.
     * Currently returns null. Future implementation will:
     * <ul>
     *   <li>Extract callsign prefix via {@link Callsign#getPrefix()}</li>
     *   <li>Look up prefix in cty.dat database</li>
     *   <li>Return accurate continent based on DXCC entity</li>
     * </ul>
     *
     * @param callsign the parsed callsign
     * @return continent code, or null (Phase 2 will implement)
     */
    public static String fromCallsign(Callsign callsign) {
        // Phase 2: Implement cty.dat-based lookup
        // For now, return null to indicate "unknown"
        return null;
    }

    /**
     * Validates a continent code.
     *
     * @param continent the continent code to validate
     * @return true if valid (AF, AN, AS, EU, NA, OC, or SA)
     */
    public static boolean isValidContinent(String continent) {
        if (continent == null) {
            return false;
        }
        return switch (continent.toUpperCase(Locale.ROOT)) {
            case "AF", "AN", "AS", "EU", "NA", "OC", "SA" -> true;
            default -> false;
        };
    }

    /**
     * Returns the full continent name for a code.
     *
     * @param continent the 2-letter continent code
     * @return full name, or null if invalid code
     */
    public static String getContinentName(String continent) {
        if (continent == null) {
            return null;
        }
        return switch (continent.toUpperCase(Locale.ROOT)) {
            case "AF" -> "Africa";
            case "AN" -> "Antarctica";
            case "AS" -> "Asia";
            case "EU" -> "Europe";
            case "NA" -> "North America";
            case "OC" -> "Oceania";
            case "SA" -> "South America";
            default -> null;
        };
    }
}
