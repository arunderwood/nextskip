package io.nextskip.propagation.model;

import java.util.Locale;

/**
 * Band condition quality ratings.
 *
 * Represents the propagation quality for a particular band:
 * - GOOD: Excellent propagation conditions
 * - FAIR: Moderate propagation conditions
 * - POOR: Poor propagation conditions
 * - UNKNOWN: Unable to determine conditions
 */
public enum BandConditionRating {
    GOOD,
    FAIR,
    POOR,
    UNKNOWN;

    /**
     * Parse a string representation to a BandConditionRating.
     *
     * @param value String value (case-insensitive)
     * @return Matching BandConditionRating, or UNKNOWN if no match
     */
    public static BandConditionRating fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return BandConditionRating.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
