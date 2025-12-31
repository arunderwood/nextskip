package io.nextskip.activations.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.activations.internal.PotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.DataRefreshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for refreshing POTA activation data.
 *
 * <p>Handles the transactional business logic for fetching POTA spots from the API,
 * persisting them to the database, cleaning up old data, and triggering cache refresh.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class PotaRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(PotaRefreshService.class);
    private static final String SERVICE_NAME = "POTA";
    private static final Duration DATA_RETENTION = Duration.ofHours(2);

    private final PotaClient potaClient;
    private final ActivationRepository repository;
    private final LoadingCache<String, List<Activation>> activationsCache;

    // Metrics for success message
    private int savedCount;
    private int deletedCount;

    public PotaRefreshService(
            PotaClient potaClient,
            ActivationRepository repository,
            LoadingCache<String, List<Activation>> activationsCache) {
        this.potaClient = potaClient;
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
        List<Activation> activations = potaClient.fetch();

        // Convert to entities
        List<ActivationEntity> entities = activations.stream()
                .map(ActivationEntity::fromDomain)
                .toList();

        // Prepare for upsert (sets IDs for existing entities to enable UPDATE)
        ActivationUpsertHelper.prepareForUpsert(entities, "POTA API", repository);

        try {
            repository.saveAll(entities);
            this.savedCount = entities.size();

            // Cleanup old data
            Instant cutoff = Instant.now().minus(DATA_RETENTION);
            this.deletedCount = repository.deleteBySpottedAtBefore(cutoff);

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during POTA refresh", e);
        }
    }

    @Override
    protected void refreshCache() {
        activationsCache.refresh(CacheConfig.CACHE_KEY);
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("POTA refresh complete: %d activations saved, %d old records deleted",
                savedCount, deletedCount);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
