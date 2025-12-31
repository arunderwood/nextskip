package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.NoaaSwpcClient;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Service for refreshing NOAA SWPC solar indices data.
 *
 * <p>Handles the transactional business logic for fetching current solar indices
 * (SFI, sunspot number) from NOAA's Space Weather Prediction Center, persisting
 * them to the database, and triggering cache refresh.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
public class NoaaRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(NoaaRefreshService.class);
    private static final String SERVICE_NAME = "NOAA";

    private final NoaaSwpcClient noaaClient;
    private final SolarIndicesRepository repository;
    private final LoadingCache<String, SolarIndices> solarIndicesCache;

    // Metrics for success message
    private double solarFluxIndex;
    private int sunspotNumber;

    public NoaaRefreshService(
            NoaaSwpcClient noaaClient,
            SolarIndicesRepository repository,
            LoadingCache<String, SolarIndices> solarIndicesCache) {
        this.noaaClient = noaaClient;
        this.repository = repository;
        this.solarIndicesCache = solarIndicesCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Fetch fresh data from API (client handles circuit breaker/retry)
        SolarIndices indices = noaaClient.fetch();

        // Store metrics for success message
        this.solarFluxIndex = indices.solarFluxIndex();
        this.sunspotNumber = indices.sunspotNumber();

        try {
            // Convert to entity and save
            SolarIndicesEntity entity = SolarIndicesEntity.fromDomain(indices);
            repository.save(entity);

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during NOAA refresh", e);
        }
    }

    @Override
    protected void refreshCache() {
        solarIndicesCache.refresh(CacheConfig.CACHE_KEY);
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("NOAA refresh complete: SFI=%.1f, Sunspots=%d",
                solarFluxIndex, sunspotNumber);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
