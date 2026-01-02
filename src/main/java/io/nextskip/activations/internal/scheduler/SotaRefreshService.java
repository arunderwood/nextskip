package io.nextskip.activations.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.activations.internal.SotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.common.scheduler.DataRefreshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for refreshing SOTA activation data.
 *
 * <p>Handles the transactional business logic for fetching SOTA spots from the API,
 * persisting them to the database, cleaning up old data, and triggering cache refresh.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class SotaRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(SotaRefreshService.class);
    private static final String SERVICE_NAME = "SOTA";
    private static final Duration DATA_RETENTION = Duration.ofHours(2);

    private final SotaClient sotaClient;
    private final ActivationRepository repository;
    private final LoadingCache<String, List<Activation>> activationsCache;

    // Metrics for success message
    private int savedCount;
    private int deletedCount;

    public SotaRefreshService(
            ApplicationEventPublisher eventPublisher,
            SotaClient sotaClient,
            ActivationRepository repository,
            LoadingCache<String, List<Activation>> activationsCache) {
        super(eventPublisher);
        this.sotaClient = sotaClient;
        this.repository = repository;
        this.activationsCache = activationsCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Fetch fresh data from API (client handles circuit breaker/retry)
        List<Activation> activations = sotaClient.fetch();

        // Convert to entities
        List<ActivationEntity> entities = activations.stream()
                .map(ActivationEntity::fromDomain)
                .toList();

        // Prepare for upsert (sets IDs for existing entities to enable UPDATE)
        ActivationUpsertHelper.prepareForUpsert(entities, "SOTA API", repository);

        try {
            repository.saveAll(entities);
            this.savedCount = entities.size();

            // Cleanup SOTA data not seen in API for 2+ hours (source-filtered to avoid race condition)
            Instant cutoff = Instant.now().minus(DATA_RETENTION);
            this.deletedCount = repository.deleteBySourceAndLastSeenAtBefore("SOTA API", cutoff);

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during SOTA refresh", e);
        }
    }

    @Override
    protected CacheRefreshEvent createCacheRefreshEvent() {
        return new CacheRefreshEvent("activations",
                () -> activationsCache.refresh(CacheConfig.CACHE_KEY));
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("SOTA refresh complete: %d activations saved, %d old records deleted",
                savedCount, deletedCount);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
