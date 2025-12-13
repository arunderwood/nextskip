package io.nextskip.propagation.api;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Public interface for the Propagation module.
 *
 * Provides access to solar indices and band condition data
 * from multiple sources (NOAA, HamQSL, etc.).
 */
public interface PropagationService {

    /**
     * Get current solar indices (SFI, K-index, A-index, sunspot number).
     *
     * @return Current solar indices, or null if unavailable
     */
    SolarIndices getCurrentSolarIndices();

    /**
     * Get band conditions for all HF amateur radio bands.
     *
     * @return List of band conditions
     */
    List<BandCondition> getBandConditions();

    /**
     * Get condition for a specific band.
     *
     * @param band The frequency band to check
     * @return Band condition, or null if unavailable
     */
    BandCondition getBandCondition(FrequencyBand band);

    /**
     * Get solar indices reactively (for streaming/WebSocket support).
     *
     * @return Mono of solar indices
     */
    Mono<SolarIndices> getSolarIndicesReactive();

    /**
     * Get band conditions reactively.
     *
     * @return Mono of band conditions list
     */
    Mono<List<BandCondition>> getBandConditionsReactive();
}
