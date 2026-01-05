package io.nextskip.spots.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.spots.api.BandActivityChangedEvent;
import io.nextskip.spots.internal.aggregation.BandActivityAggregator;
import io.nextskip.spots.model.BandActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for refreshing band activity aggregations.
 *
 * <p>Unlike other refresh services that fetch data from external APIs,
 * this service aggregates existing spot data from the database. It:
 * <ol>
 *   <li>Triggers aggregation via {@link BandActivityAggregator}</li>
 *   <li>Publishes {@link BandActivityChangedEvent} for other modules</li>
 *   <li>Refreshes the band activity cache</li>
 * </ol>
 *
 * <p>Runs every minute to keep band activity data fresh for the dashboard.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BandActivityRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(BandActivityRefreshService.class);
    private static final String SERVICE_NAME = "Band Activity";

    private final BandActivityAggregator aggregator;
    private final LoadingCache<String, Map<String, BandActivity>> bandActivityCache;
    private final ApplicationEventPublisher eventPublisher;

    // Metrics for success message
    private int bandsProcessed;
    private int totalSpots;

    public BandActivityRefreshService(
            ApplicationEventPublisher eventPublisher,
            BandActivityAggregator aggregator,
            LoadingCache<String, Map<String, BandActivity>> bandActivityCache) {
        super(eventPublisher);
        this.eventPublisher = eventPublisher;
        this.aggregator = aggregator;
        this.bandActivityCache = bandActivityCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Aggregate all bands with recent activity
        Map<String, BandActivity> activities = aggregator.aggregateAllBands();

        // Update metrics for logging
        this.bandsProcessed = activities.size();
        this.totalSpots = activities.values().stream()
                .mapToInt(BandActivity::spotCount)
                .sum();

        // Publish change event for inter-module notification
        eventPublisher.publishEvent(new BandActivityChangedEvent(activities));
    }

    @Override
    protected CacheRefreshEvent createCacheRefreshEvent() {
        return new CacheRefreshEvent("bandActivity",
                () -> bandActivityCache.refresh(CacheConfig.CACHE_KEY));
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("Band activity refresh complete: %d bands, %d total spots",
                bandsProcessed, totalSpots);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
