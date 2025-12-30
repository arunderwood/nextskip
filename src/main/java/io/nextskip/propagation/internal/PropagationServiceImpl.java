package io.nextskip.propagation.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.api.PropagationResponse;
import io.nextskip.propagation.api.PropagationService;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Implementation of PropagationService.
 *
 * <p>Reads propagation data from LoadingCaches backed by the database.
 * Caches are populated by scheduler tasks:
 * - NoaaRefreshTask and HamQslSolarRefreshTask for solar indices
 * - HamQslBandRefreshTask for band conditions
 *
 * <p>Solar indices are automatically merged by the cache loader (NOAA for
 * SFI/sunspots, HamQSL for K/A indices).
 */
@Service
public class PropagationServiceImpl implements PropagationService {

    private static final Logger LOG = LoggerFactory.getLogger(PropagationServiceImpl.class);

    private final LoadingCache<String, SolarIndices> solarIndicesCache;
    private final LoadingCache<String, List<BandCondition>> bandConditionsCache;

    public PropagationServiceImpl(
            LoadingCache<String, SolarIndices> solarIndicesCache,
            LoadingCache<String, List<BandCondition>> bandConditionsCache) {
        this.solarIndicesCache = solarIndicesCache;
        this.bandConditionsCache = bandConditionsCache;
    }

    /**
     * Get current solar indices from cache.
     *
     * <p>Returns merged data from NOAA SWPC and HamQSL (merging done by cache loader).
     */
    @Override
    public SolarIndices getCurrentSolarIndices() {
        LOG.debug("Fetching solar indices from cache");
        SolarIndices indices = solarIndicesCache.get(CacheConfig.CACHE_KEY);
        if (indices != null) {
            LOG.debug("Retrieved solar indices: SFI={}, K={}",
                    indices.solarFluxIndex(), indices.kIndex());
        } else {
            LOG.warn("No solar indices available in cache");
        }
        return indices;
    }

    /**
     * Get current band conditions from cache.
     */
    @Override
    public List<BandCondition> getBandConditions() {
        LOG.debug("Fetching band conditions from cache");
        List<BandCondition> conditions = bandConditionsCache.get(CacheConfig.CACHE_KEY);
        if (conditions == null) {
            conditions = List.of();
        }
        LOG.debug("Retrieved {} band conditions from cache", conditions.size());
        return conditions;
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

    @Override
    public PropagationResponse getPropagationResponse() {
        LOG.debug("Building propagation response for dashboard");

        SolarIndices solarIndices = getCurrentSolarIndices();
        List<BandCondition> bandConditions = getBandConditions();

        PropagationResponse response = new PropagationResponse(solarIndices, bandConditions);

        LOG.debug("Returning propagation response: {} band conditions",
                bandConditions.size());

        return response;
    }
}
