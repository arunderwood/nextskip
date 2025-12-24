package io.nextskip.activations.internal;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps SOTA association codes to US state abbreviations.
 *
 * <p>SOTA uses association codes like "W7W" (Washington), "W4G" (Georgia), etc.
 * This mapper extracts the state abbreviation for display purposes.
 *
 * <p>This is a static mapping because:
 * <ul>
 *   <li>SOTA associations are relatively stable (states don't change)</li>
 *   <li>Avoids additional API calls for every activation</li>
 *   <li>The spots API doesn't include detailed state information</li>
 * </ul>
 *
 * <p>For non-US associations or unknown codes, this mapper returns empty.
 */
public final class SotaAssociationMapper {

    private static final Map<String, String> ASSOCIATION_TO_STATE = Map.ofEntries(
            // W1 region (New England)
            Map.entry("W1", "CT"),   // Connecticut (primary)
            Map.entry("W1/CT", "CT"),
            Map.entry("W1/MA", "MA"),
            Map.entry("W1/ME", "ME"),
            Map.entry("W1/NH", "NH"),
            Map.entry("W1/RI", "RI"),
            Map.entry("W1/VT", "VT"),

            // W2 region (New York, New Jersey)
            Map.entry("W2", "NY"),   // New York (primary)
            Map.entry("W2/NJ", "NJ"),
            Map.entry("W2/NY", "NY"),

            // W3 region (Mid-Atlantic)
            Map.entry("W3", "PA"),   // Pennsylvania (primary)
            Map.entry("W3/PA", "PA"),
            Map.entry("W3/DE", "DE"),
            Map.entry("W3/MD", "MD"),

            // W4 region (Southeast)
            Map.entry("W4A", "AL"),  // Alabama
            Map.entry("W4C", "NC"),  // North/South Carolina
            Map.entry("W4G", "GA"),  // Georgia
            Map.entry("W4K", "KY"),  // Kentucky
            Map.entry("W4T", "TN"),  // Tennessee
            Map.entry("W4V", "VA"),  // Virginia

            // W5 region (South Central)
            Map.entry("W5A", "AR"),  // Arkansas
            Map.entry("W5L", "LA"),  // Louisiana
            Map.entry("W5M", "MS"),  // Mississippi
            Map.entry("W5N", "NM"),  // New Mexico
            Map.entry("W5O", "OK"),  // Oklahoma
            Map.entry("W5T", "TX"),  // Texas

            // W6 region (California)
            Map.entry("W6", "CA"),   // California

            // W7 region (Pacific Northwest / Mountain)
            Map.entry("W7A", "AZ"),  // Arizona
            Map.entry("W7I", "ID"),  // Idaho
            Map.entry("W7M", "MT"),  // Montana
            Map.entry("W7N", "NV"),  // Nevada
            Map.entry("W7O", "OR"),  // Oregon
            Map.entry("W7U", "UT"),  // Utah
            Map.entry("W7W", "WA"),  // Washington
            Map.entry("W7Y", "WY"),  // Wyoming

            // W8 region (Great Lakes)
            Map.entry("W8M", "MI"),  // Michigan
            Map.entry("W8O", "OH"),  // Ohio
            Map.entry("W8V", "WV"),  // West Virginia

            // W9 region (Central)
            Map.entry("W9", "IL"),   // Illinois (primary)
            Map.entry("W9/IL", "IL"),
            Map.entry("W9/IN", "IN"),
            Map.entry("W9/WI", "WI"),

            // W0 region (Central Plains / Upper Midwest)
            Map.entry("W0C", "CO"),  // Colorado
            Map.entry("W0K", "KS"),  // Kansas
            Map.entry("W0M", "MN"),  // Minnesota
            Map.entry("W0N", "NE"),  // Nebraska
            Map.entry("W0D", "SD"),  // Dakotas (South Dakota primary)
            Map.entry("W0S", "SD"),  // South Dakota
            Map.entry("W0I", "IA"),  // Iowa
            Map.entry("Wø", "MO")    // Missouri (using alternative zero character)
    );

    private SotaAssociationMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Extract state abbreviation from SOTA association code.
     *
     * <p>Examples:
     * <ul>
     *   <li>"W7W" → "WA" (Washington)</li>
     *   <li>"W4G" → "GA" (Georgia)</li>
     *   <li>"W0C" → "CO" (Colorado)</li>
     *   <li>"VK2" → empty (non-US association)</li>
     * </ul>
     *
     * @param associationCode The SOTA association code (e.g., "W7W", "W4G")
     * @return Optional containing state abbreviation, or empty if unknown
     */
    public static Optional<String> toStateCode(String associationCode) {
        if (associationCode == null || associationCode.isBlank()) {
            return Optional.empty();
        }

        String code = associationCode.toUpperCase(Locale.ROOT).trim();
        return Optional.ofNullable(ASSOCIATION_TO_STATE.get(code));
    }
}
