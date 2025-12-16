package io.nextskip.activations.model;

/**
 * Represents a Summits on the Air (SOTA) summit location.
 *
 * <p>This record contains location-specific data for SOTA summits,
 * including the association code that groups summits by region.
 *
 * <p>Implements {@link ActivationLocation} to provide a common interface
 * with other activation location types.
 *
 * @param reference Summit reference code (e.g., "W7W/LC-001", "W4G/NG-001")
 * @param name Human-readable summit name (e.g., "Mount Saint Helens", "Brasstown Bald")
 * @param regionCode State or region abbreviation (e.g., "WA", "GA"), or null if unknown
 * @param associationCode SOTA association code (e.g., "W7W", "W4G"), or null if unknown
 */
public record Summit(
        String reference,
        String name,
        String regionCode,
        String associationCode
) implements ActivationLocation {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if reference or name is null
     */
    public Summit {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Summit reference cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Summit name cannot be null or blank");
        }
    }
}
