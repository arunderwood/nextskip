package io.nextskip.activations.model;

/**
 * Represents a Parks on the Air (POTA) park location.
 *
 * <p>This record contains location-specific data for POTA parks,
 * including geographic coordinates and grid locator information.
 *
 * <p>Implements {@link ActivationLocation} to provide a common interface
 * with other activation location types.
 *
 * @param reference Park reference code (e.g., "K-0817", "US-0001")
 * @param name Human-readable park name (e.g., "Rocky Mountain National Park")
 * @param regionCode State or region abbreviation (e.g., "CO", "MS"), or null if unknown
 * @param countryCode Country code (e.g., "US", "K"), or null if unknown
 * @param grid Maidenhead grid square locator (e.g., "DM79"), or null if unknown
 * @param latitude Latitude in decimal degrees, or null if unknown
 * @param longitude Longitude in decimal degrees, or null if unknown
 */
public record Park(
        String reference,
        String name,
        String regionCode,
        String countryCode,
        String grid,
        Double latitude,
        Double longitude
) implements ActivationLocation {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if reference or name is null
     */
    public Park {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Park reference cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Park name cannot be null or blank");
        }
    }
}
