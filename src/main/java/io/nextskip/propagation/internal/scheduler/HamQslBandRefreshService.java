package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.HamQslBandClient;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for refreshing HamQSL band condition data.
 *
 * <p>Handles the transactional business logic for fetching HF band conditions
 * from HamQSL, persisting them to the database, and triggering cache refresh.
 * Provides band-by-band propagation ratings (Good/Fair/Poor).
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
public class HamQslBandRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(HamQslBandRefreshService.class);
    private static final String SERVICE_NAME = "HamQSL Band";

    private final HamQslBandClient hamQslBandClient;
    private final BandConditionRepository repository;
    private final LoadingCache<String, List<BandCondition>> bandConditionsCache;

    // Metrics for success message
    private int savedCount;

    public HamQslBandRefreshService(
            ApplicationEventPublisher eventPublisher,
            HamQslBandClient hamQslBandClient,
            BandConditionRepository repository,
            LoadingCache<String, List<BandCondition>> bandConditionsCache) {
        super(eventPublisher);
        this.hamQslBandClient = hamQslBandClient;
        this.repository = repository;
        this.bandConditionsCache = bandConditionsCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Fetch fresh data from API (client handles circuit breaker/retry)
        List<BandCondition> conditions = hamQslBandClient.fetch();

        // Convert to entities
        Instant now = Instant.now();
        List<BandConditionEntity> entities = conditions.stream()
                .map(bc -> BandConditionEntity.fromDomain(bc, now))
                .toList();

        try {
            repository.saveAll(entities);
            this.savedCount = entities.size();

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during HamQSL band refresh", e);
        }
    }

    @Override
    protected CacheRefreshEvent createCacheRefreshEvent() {
        return new CacheRefreshEvent("bandConditions",
                () -> bandConditionsCache.refresh(CacheConfig.CACHE_KEY));
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("HamQSL band refresh complete: %d band conditions saved", savedCount);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
