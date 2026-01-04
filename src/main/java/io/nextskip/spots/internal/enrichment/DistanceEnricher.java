package io.nextskip.spots.internal.enrichment;

import io.nextskip.common.model.Coordinates;
import io.nextskip.common.model.GridSquare;
import io.nextskip.spots.model.Spot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enriches spots with distance calculated from grid squares.
 *
 * <p>Uses the Haversine formula via {@link Coordinates#distanceTo(Coordinates)}
 * to calculate great-circle distance between spotter and spotted stations.
 *
 * <p>Distance calculation requires valid grid squares for both stations.
 * If either grid is missing or invalid, the spot is returned unchanged.
 *
 * <p>Example distances:
 * <ul>
 *   <li>FN31 (Connecticut) to JO01 (UK): ~5,500 km</li>
 *   <li>CM97 (California) to PM95 (Japan): ~8,500 km</li>
 *   <li>FN31 (Connecticut) to CM97 (California): ~4,000 km</li>
 * </ul>
 */
@Component
public class DistanceEnricher implements SpotEnricher {

    private static final Logger LOG = LoggerFactory.getLogger(DistanceEnricher.class);

    @Override
    public Spot enrich(Spot spot) {
        if (spot == null) {
            return null;
        }

        // Skip if already enriched
        if (spot.distanceKm() != null) {
            return spot;
        }

        String spotterGrid = spot.spotterGrid();
        String spottedGrid = spot.spottedGrid();

        // Need both grids for distance calculation
        if (spotterGrid == null || spotterGrid.isBlank()
                || spottedGrid == null || spottedGrid.isBlank()) {
            return spot;
        }

        try {
            GridSquare spotterGridSquare = new GridSquare(spotterGrid);
            GridSquare spottedGridSquare = new GridSquare(spottedGrid);

            Coordinates spotterCoords = spotterGridSquare.toCoordinates();
            Coordinates spottedCoords = spottedGridSquare.toCoordinates();

            double distanceKm = spotterCoords.distanceTo(spottedCoords);
            int roundedDistance = (int) Math.round(distanceKm);

            return spot.withDistance(roundedDistance);
        } catch (IllegalArgumentException e) {
            // Invalid grid square format - log at debug level (expected for some data)
            LOG.debug("Could not calculate distance for grids {} -> {}: {}",
                    spotterGrid, spottedGrid, e.getMessage());
            return spot;
        }
    }
}
