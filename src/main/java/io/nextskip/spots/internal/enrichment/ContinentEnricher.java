package io.nextskip.spots.internal.enrichment;

import io.nextskip.common.model.ContinentLookup;
import io.nextskip.spots.model.Spot;
import org.springframework.stereotype.Component;

/**
 * Enriches spots with continent codes derived from grid squares.
 *
 * <p>Delegates to {@link ContinentLookup#fromGridSquare(String)} for continent
 * determination. Accuracy is approximately 80% due to grid field boundaries
 * not aligning perfectly with continental boundaries.
 *
 * <p>Phase 2 will enhance accuracy using cty.dat-based callsign prefix lookup.
 *
 * <p>Continent codes follow standard 2-letter abbreviations:
 * <ul>
 *   <li>AF - Africa</li>
 *   <li>AN - Antarctica</li>
 *   <li>AS - Asia</li>
 *   <li>EU - Europe</li>
 *   <li>NA - North America</li>
 *   <li>OC - Oceania</li>
 *   <li>SA - South America</li>
 * </ul>
 */
@Component
public class ContinentEnricher implements SpotEnricher {

    @Override
    public Spot enrich(Spot spot) {
        if (spot == null) {
            return null;
        }

        // Skip if already enriched
        if (spot.spotterContinent() != null && spot.spottedContinent() != null) {
            return spot;
        }

        String spotterContinent = spot.spotterContinent();
        String spottedContinent = spot.spottedContinent();

        // Enrich spotter continent if missing
        if (spotterContinent == null && spot.spotterGrid() != null) {
            spotterContinent = ContinentLookup.fromGridSquare(spot.spotterGrid());
        }

        // Enrich spotted continent if missing
        if (spottedContinent == null && spot.spottedGrid() != null) {
            spottedContinent = ContinentLookup.fromGridSquare(spot.spottedGrid());
        }

        // Only create new instance if we actually enriched something
        boolean spotterChanged = !java.util.Objects.equals(spotterContinent, spot.spotterContinent());
        boolean spottedChanged = !java.util.Objects.equals(spottedContinent, spot.spottedContinent());
        if (spotterChanged || spottedChanged) {
            return spot.withContinents(spotterContinent, spottedContinent);
        }

        return spot;
    }
}
