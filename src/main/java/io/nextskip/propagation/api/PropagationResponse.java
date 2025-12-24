package io.nextskip.propagation.api;

import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for propagation data.
 *
 * Combines solar indices and band conditions into a single response
 * for the dashboard frontend.
 *
 * @param solarIndices   Current solar activity indices
 * @param bandConditions Propagation conditions for each HF band
 * @param timestamp      When this data was generated
 */
public record PropagationResponse(
        SolarIndices solarIndices,
        List<BandCondition> bandConditions,
        Instant timestamp
) {
    /**
     * Compact constructor for defensive copying of mutable collections.
     */
    public PropagationResponse {
        bandConditions = bandConditions != null ? List.copyOf(bandConditions) : List.of();
    }

    /**
     * Create a PropagationResponse with current timestamp.
     */
    public PropagationResponse(SolarIndices solarIndices, List<BandCondition> bandConditions) {
        this(solarIndices, bandConditions, Instant.now());
    }
}
