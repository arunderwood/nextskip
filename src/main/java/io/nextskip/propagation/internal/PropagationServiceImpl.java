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
 * - HamQSL: K-index, A-index, band conditions
 *
 * The service combines data from both sources to provide comprehensive
 * propagation information.
 */
@Service
public class PropagationServiceImpl implements PropagationService {

    private static final Logger log = LoggerFactory.getLogger(PropagationServiceImpl.class);

    private final NoaaSwpcClient noaaClient;
    private final HamQslClient hamQslClient;

    public PropagationServiceImpl(NoaaSwpcClient noaaClient, HamQslClient hamQslClient) {
        this.noaaClient = noaaClient;
        this.hamQslClient = hamQslClient;
    }

    @Override
    public SolarIndices getCurrentSolarIndices() {
        try {
            // Fetch from both sources
            SolarIndices noaaData = noaaClient.fetchSolarIndices();
            SolarIndices hamQslData = hamQslClient.fetchSolarIndices();

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
                log.warn("HamQSL data unavailable, using NOAA data only");
                return noaaData;
            } else if (hamQslData != null) {
                log.warn("NOAA data unavailable, using HamQSL data only");
                return hamQslData;
            } else {
                log.error("Both NOAA and HamQSL data unavailable");
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching solar indices", e);
            return null;
        }
    }

    @Override
    public List<BandCondition> getBandConditions() {
        try {
            List<BandCondition> conditions = hamQslClient.fetchBandConditions();
            log.debug("Retrieved {} band conditions", conditions.size());
            return conditions;
        } catch (Exception e) {
            log.error("Error fetching band conditions", e);
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
