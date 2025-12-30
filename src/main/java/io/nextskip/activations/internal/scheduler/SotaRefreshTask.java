package io.nextskip.activations.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.activations.internal.SotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.DataRefreshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Recurring task for refreshing SOTA activation data.
 *
 * <p>Fetches recent SOTA spots from the API, persists them to the database,
 * and triggers an async cache refresh. Old data is automatically cleaned up.
 *
 * <p>Task runs every 1 minute (matching the original refresh interval).
 */
@Configuration
public class SotaRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(SotaRefreshTask.class);
    private static final String TASK_NAME = "sota-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration DATA_RETENTION = Duration.ofHours(2);

    /**
     * Creates the recurring task bean for SOTA data refresh.
     *
     * @param sotaClient       the SOTA API client
     * @param repository       the activation repository for persistence
     * @param activationsCache the LoadingCache to refresh after DB write
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> sotaRecurringTask(
            SotaClient sotaClient,
            ActivationRepository repository,
            LoadingCache<String, List<Activation>> activationsCache) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(sotaClient, repository, activationsCache));
    }

    /**
     * Executes the SOTA data refresh.
     *
     * <p>Fetches data from API, saves to database, then triggers async cache refresh.
     * The cache refresh is non-blocking - old cached value is served during refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param sotaClient       the SOTA API client
     * @param repository       the activation repository
     * @param activationsCache the cache to refresh
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(
            SotaClient sotaClient,
            ActivationRepository repository,
            LoadingCache<String, List<Activation>> activationsCache) {

        LOG.debug("Executing SOTA refresh task");

        try {
            // Fetch fresh data from API
            List<Activation> activations = sotaClient.fetch();

            // Convert to entities and save
            List<ActivationEntity> entities = activations.stream()
                    .map(ActivationEntity::fromDomain)
                    .toList();

            repository.saveAll(entities);

            // Cleanup old data
            Instant cutoff = Instant.now().minus(DATA_RETENTION);
            int deleted = repository.deleteBySpottedAtBefore(cutoff);

            // Trigger async cache refresh (non-blocking)
            activationsCache.refresh(CacheConfig.CACHE_KEY);

            LOG.info("SOTA refresh complete: {} activations saved, {} old records deleted, cache refresh triggered",
                    entities.size(), deleted);

        } catch (Exception e) {
            LOG.error("SOTA refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("SOTA refresh failed", e);
        }
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the activation repository
     * @return true if no recent SOTA data exists
     */
    public boolean needsInitialLoad(ActivationRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(5));
        return repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                ActivationType.SOTA, recent).isEmpty();
    }
}
