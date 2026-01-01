package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.HamQslSolarClient;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Service for refreshing HamQSL solar indices data.
 *
 * <p>Handles the transactional business logic for fetching solar indices
 * (SFI, K-index, A-index, sunspots) from HamQSL, persisting them to the
 * database, and triggering cache refresh. HamQSL provides K and A indices
 * which complement NOAA's SFI/sunspots.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
public class HamQslSolarRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(HamQslSolarRefreshService.class);
    private static final String SERVICE_NAME = "HamQSL Solar";

    private final HamQslSolarClient hamQslSolarClient;
    private final SolarIndicesRepository repository;
    private final LoadingCache<String, SolarIndices> solarIndicesCache;

    // Metrics for success message
    private Integer kIndex;
    private Integer aIndex;
    private boolean skipped;

    public HamQslSolarRefreshService(
            ApplicationEventPublisher eventPublisher,
            HamQslSolarClient hamQslSolarClient,
            SolarIndicesRepository repository,
            LoadingCache<String, SolarIndices> solarIndicesCache) {
        super(eventPublisher);
        this.hamQslSolarClient = hamQslSolarClient;
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
        SolarIndices indices = hamQslSolarClient.fetch();

        if (indices == null) {
            LOG.warn("HamQSL solar client returned null - skipping save");
            this.skipped = true;
            return;
        }

        this.skipped = false;
        this.kIndex = indices.kIndex();
        this.aIndex = indices.aIndex();

        try {
            // Convert to entity and save
            SolarIndicesEntity entity = SolarIndicesEntity.fromDomain(indices);
            repository.save(entity);

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during HamQSL solar refresh", e);
        }
    }

    @Override
    protected CacheRefreshEvent createCacheRefreshEvent() {
        // Only refresh cache if data was actually saved
        if (skipped) {
            return new CacheRefreshEvent("solarIndices (skipped)", () -> {});
        }
        return new CacheRefreshEvent("solarIndices",
                () -> solarIndicesCache.refresh(CacheConfig.CACHE_KEY));
    }

    @Override
    protected String getSuccessMessage() {
        if (skipped) {
            return "HamQSL solar refresh skipped: null response from client";
        }
        return String.format("HamQSL solar refresh complete: K=%d, A=%d", kIndex, aIndex);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
