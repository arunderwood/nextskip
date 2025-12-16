package io.nextskip.activations.model;

/**
 * Common interface for activation locations (parks and summits).
 *
 * <p>This interface provides location-specific data for amateur radio activations,
 * abstracting the differences between POTA parks and SOTA summits.
 *
 * <p>Design Benefits (SOLID):
 * <ul>
 *   <li><b>Single Responsibility</b>: Separates location data from activation events</li>
 *   <li><b>Open-Closed</b>: New location types can be added without modifying existing code</li>
 *   <li><b>Liskov Substitution</b>: Park and Summit are interchangeable via this interface</li>
 *   <li><b>Dependency Inversion</b>: Activation depends on abstraction, not concretions</li>
 * </ul>
 *
 * @see Park
 * @see Summit
 */
public interface ActivationLocation {

    /**
     * Gets the unique reference code for this location.
     *
     * @return reference code (e.g., "K-0817" for POTA, "W7W/LC-001" for SOTA)
     */
    String reference();

    /**
     * Gets the human-readable name of this location.
     *
     * @return location name (e.g., "Rocky Mountain National Park", "Mount Saint Helens")
     */
    String name();

    /**
     * Gets the state or region code for this location.
     *
     * @return state/region abbreviation (e.g., "CO", "WA"), or null if unknown
     */
    String regionCode();
}
