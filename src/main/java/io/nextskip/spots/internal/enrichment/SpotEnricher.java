package io.nextskip.spots.internal.enrichment;

import io.nextskip.spots.model.Spot;

/**
 * Functional interface for enriching spots with derived data.
 *
 * <p>Enrichers add computed fields to spots such as distance calculations
 * or continent lookups. They follow the decorator pattern, returning
 * new immutable Spot instances with additional data.
 *
 * <p>Enrichers are designed to be composed in a pipeline:
 * <pre>{@code
 * Spot enriched = spot
 *     .transform(distanceEnricher::enrich)
 *     .transform(continentEnricher::enrich);
 * }</pre>
 *
 * <p>Implementations should be null-safe and handle missing input gracefully
 * by returning the original spot unchanged when enrichment is not possible.
 *
 * @see DistanceEnricher
 * @see ContinentEnricher
 */
@FunctionalInterface
public interface SpotEnricher {

    /**
     * Enriches a spot with derived data.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Return a new Spot with additional data when enrichment succeeds</li>
     *   <li>Return the original spot unchanged when enrichment is not possible</li>
     *   <li>Never throw exceptions - handle errors gracefully</li>
     * </ul>
     *
     * @param spot the spot to enrich
     * @return enriched spot (may be same instance if no enrichment possible)
     */
    Spot enrich(Spot spot);
}
