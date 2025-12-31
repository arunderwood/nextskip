package io.nextskip.meteors.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.meteors.internal.MeteorShowerDataLoader;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for refreshing meteor shower data.
 *
 * <p>Handles the transactional business logic for loading meteor shower data
 * from the curated JSON file, computing concrete dates for the current and
 * upcoming year, persisting them to the database, and triggering cache refresh.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
public class MeteorRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorRefreshService.class);
    private static final String SERVICE_NAME = "Meteor";
    private static final int LOOKAHEAD_DAYS = 30;

    private final MeteorShowerDataLoader dataLoader;
    private final MeteorShowerRepository repository;
    private final LoadingCache<String, List<MeteorShower>> meteorShowersCache;

    // Metrics for success message
    private int savedCount;

    public MeteorRefreshService(
            MeteorShowerDataLoader dataLoader,
            MeteorShowerRepository repository,
            LoadingCache<String, List<MeteorShower>> meteorShowersCache) {
        this.dataLoader = dataLoader;
        this.repository = repository;
        this.meteorShowersCache = meteorShowersCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Load shower data with computed dates for current period
        List<MeteorShower> showers = dataLoader.getShowers(LOOKAHEAD_DAYS);

        // Convert to entities
        List<MeteorShowerEntity> entities = showers.stream()
                .map(MeteorShowerEntity::fromDomain)
                .toList();

        try {
            // Clear old data and save new (shower dates change annually)
            repository.deleteAll();
            repository.saveAll(entities);
            this.savedCount = entities.size();

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during meteor refresh", e);
        }
    }

    @Override
    protected void refreshCache() {
        meteorShowersCache.refresh(CacheConfig.CACHE_KEY);
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("Meteor refresh complete: %d showers saved", savedCount);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
