package io.nextskip.propagation.internal;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.api.PropagationService;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of PropagationService that aggregates data from multiple sources.
 *
 * Data sources:
 * - NOAA SWPC: Solar flux index, sunspot number
 * - HamQSL Solar: K-index, A-index
 * - HamQSL Band: Band conditions
 *
 * The service combines data from both sources to provide comprehensive
 * propagation information.
 */
@Service
public class PropagationServiceImpl implements PropagationService {

    private static final Logger LOG = LoggerFactory.getLogger(PropagationServiceImpl.class);

    private final NoaaSwpcClient noaaClient;
    private final HamQslSolarClient hamQslSolarClient;
    private final HamQslBandClient hamQslBandClient;

    public PropagationServiceImpl(
            NoaaSwpcClient noaaClient,
            HamQslSolarClient hamQslSolarClient,
            HamQslBandClient hamQslBandClient) {
        this.noaaClient = noaaClient;
        this.hamQslSolarClient = hamQslSolarClient;
        this.hamQslBandClient = hamQslBandClient;
    }

    /**
     * Get current solar indices by merging data from NOAA SWPC and HamQSL.
     *
     * <p>Primary error handling via clients' circuit breakers.
     * Service-level fallback ensures dashboard remains functional.
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public SolarIndices getCurrentSolarIndices() {
        try {
            // Fetch from both sources
            SolarIndices noaaData = noaaClient.fetch();
            SolarIndices hamQslData = hamQslSolarClient.fetch();

            // Merge data: prefer NOAA for SFI/sunspots, HamQSL for K/A index
            if (noaaData != null && hamQslData != null) {
                return new SolarIndices(
                        noaaData.solarFluxIndex(),  // From NOAA
                        hamQslData.aIndex(),        // From HamQSL
                        hamQslData.kIndex(),        // From HamQSL
                        noaaData.sunspotNumber(),   // From NOAA
                        Instant.now(),
                        "NOAA SWPC + HamQSL"
                );
            } else if (noaaData != null) {
                LOG.warn("HamQSL data unavailable, using NOAA data only");
                return noaaData;
            } else if (hamQslData != null) {
                LOG.warn("NOAA data unavailable, using HamQSL data only");
                return hamQslData;
            } else {
                LOG.error("Both NOAA and HamQSL data unavailable");
                return null;
            }
        } catch (RuntimeException e) {
            LOG.error("Error fetching solar indices: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get current band conditions from HamQSL.
     *
     * <p>Primary error handling via HamQslBandClient's circuit breaker.
     * Service-level fallback ensures dashboard remains functional.
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public List<BandCondition> getBandConditions() {
        try {
            List<BandCondition> conditions = hamQslBandClient.fetch();
            LOG.debug("Retrieved {} band conditions", conditions.size());
            return conditions;
        } catch (RuntimeException e) {
            LOG.error("Error fetching band conditions: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public BandCondition getBandCondition(FrequencyBand band) {
        if (band == null) {
            return null;
        }

        List<BandCondition> conditions = getBandConditions();
        return conditions.stream()
                .filter(c -> c.band() == band)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Mono<SolarIndices> getSolarIndicesReactive() {
        return Mono.fromCallable(this::getCurrentSolarIndices);
    }

    @Override
    public Mono<List<BandCondition>> getBandConditionsReactive() {
        return Mono.fromCallable(this::getBandConditions);
    }
}
