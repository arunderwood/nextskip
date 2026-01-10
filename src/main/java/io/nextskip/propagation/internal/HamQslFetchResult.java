package io.nextskip.propagation.internal;

import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;

import java.util.List;

/**
 * Result of a unified HamQSL fetch containing both solar indices and band conditions.
 *
 * <p>This record is used by {@link HamQslClient} to return all data from a single
 * HTTP request to the HamQSL XML feed, eliminating duplicate fetches.
 *
 * @param solarIndices the solar indices (SFI, K-index, A-index, sunspots)
 * @param bandConditions the HF band conditions (80m-10m ratings)
 */
public record HamQslFetchResult(
        SolarIndices solarIndices,
        List<BandCondition> bandConditions
) {
    /**
     * Canonical constructor that creates a defensive copy of the band conditions list.
     *
     * @param solarIndices the solar indices
     * @param bandConditions the band conditions (will be copied to ensure immutability)
     */
    public HamQslFetchResult {
        bandConditions = bandConditions == null ? List.of() : List.copyOf(bandConditions);
    }
}
