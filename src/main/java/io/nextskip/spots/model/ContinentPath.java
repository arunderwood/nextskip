package io.nextskip.spots.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Major HF propagation paths between continents.
 *
 * <p>These represent the key long-distance paths that amateur radio operators
 * are most interested in monitoring. Each path is bidirectional - a spot from
 * NA to EU is the same path as EU to NA.
 *
 * <p>The six major paths cover the most common DX communications:
 * <ul>
 *   <li>Trans-Atlantic (NA↔EU) - Most active path, especially on 20m</li>
 *   <li>Trans-Pacific (NA↔AS) - Long path, often via polar or long path</li>
 *   <li>Europe-Asia (EU↔AS) - Important for EU operators targeting JA</li>
 *   <li>North America-Oceania (NA↔OC) - VK/ZL contacts</li>
 *   <li>Europe-Africa (EU↔AF) - Short to medium distance</li>
 *   <li>North-South America (NA↔SA) - Often good on 10m/15m</li>
 * </ul>
 */
public enum ContinentPath {

    /**
     * Trans-Atlantic path between North America and Europe.
     */
    NA_EU("NA", "EU", "Trans-Atlantic"),

    /**
     * Trans-Pacific path between North America and Asia.
     */
    NA_AS("NA", "AS", "Trans-Pacific"),

    /**
     * Path between Europe and Asia.
     */
    EU_AS("EU", "AS", "Europe-Asia"),

    /**
     * Path between North America and Oceania.
     */
    NA_OC("NA", "OC", "North America-Oceania"),

    /**
     * Path between Europe and Africa.
     */
    EU_AF("EU", "AF", "Europe-Africa"),

    /**
     * Path between North and South America.
     */
    NA_SA("NA", "SA", "North-South America");

    private final String continent1;
    private final String continent2;
    private final String displayName;

    ContinentPath(String continent1, String continent2, String displayName) {
        this.continent1 = continent1;
        this.continent2 = continent2;
        this.displayName = displayName;
    }

    /**
     * Returns the first continent code in this path.
     *
     * @return 2-letter continent code (e.g., "NA")
     */
    public String getContinent1() {
        return continent1;
    }

    /**
     * Returns the second continent code in this path.
     *
     * @return 2-letter continent code (e.g., "EU")
     */
    public String getContinent2() {
        return continent2;
    }

    /**
     * Returns the human-readable display name for this path.
     *
     * @return display name (e.g., "Trans-Atlantic")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the enum name for JSON serialization.
     *
     * <p>This ensures the enum is serialized as "NA_EU" not "Trans-Atlantic (NA↔EU)".
     *
     * @return the enum constant name (e.g., "NA_EU")
     */
    @JsonValue
    public String toJsonValue() {
        return name();
    }

    /**
     * Checks if this path matches the given continent pair.
     *
     * <p>The match is bidirectional - (NA, EU) matches NA_EU just as (EU, NA) does.
     * Comparison is case-insensitive.
     *
     * @param spotterContinent the continent of the receiving station
     * @param spottedContinent the continent of the transmitting station
     * @return true if this path connects the two continents
     */
    public boolean matches(String spotterContinent, String spottedContinent) {
        if (spotterContinent == null || spottedContinent == null) {
            return false;
        }

        String c1 = spotterContinent.toUpperCase(Locale.ROOT);
        String c2 = spottedContinent.toUpperCase(Locale.ROOT);

        return continent1.equals(c1) && continent2.equals(c2)
                || continent2.equals(c1) && continent1.equals(c2);
    }

    /**
     * Finds the ContinentPath that matches the given continent pair.
     *
     * <p>The match is bidirectional and case-insensitive.
     *
     * @param continent1 first continent code
     * @param continent2 second continent code
     * @return the matching path, or empty if no major path connects these continents
     */
    public static Optional<ContinentPath> fromContinents(String continent1, String continent2) {
        if (continent1 == null || continent2 == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(path -> path.matches(continent1, continent2))
                .findFirst();
    }

    /**
     * Returns a formatted string showing the path direction.
     *
     * @return formatted path (e.g., "NA↔EU")
     */
    public String toPathString() {
        return continent1 + "↔" + continent2;
    }

    @Override
    public String toString() {
        return displayName + " (" + toPathString() + ")";
    }
}
