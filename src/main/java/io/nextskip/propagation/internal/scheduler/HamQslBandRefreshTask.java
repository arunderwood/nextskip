package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.HamQslBandClient;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Recurring task for refreshing HamQSL band condition data.
 *
 * <p>Fetches HF band conditions from HamQSL, persists them to the database,
 * and triggers an async cache refresh. Provides band-by-band propagation
 * ratings (Good/Fair/Poor).
 *
 * <p>Task runs every 15 minutes (slightly more frequent than solar data
 * since band conditions can change more quickly).
 */
@Configuration
public class HamQslBandRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(HamQslBandRefreshTask.class);
    private static final String TASK_NAME = "hamqsl-band-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(15);

    /**
     * Creates the recurring task bean for HamQSL band condition refresh.
     *
     * @param hamQslBandClient    the HamQSL band API client
     * @param repository          the band condition repository for persistence
     * @param bandConditionsCache the LoadingCache to refresh after DB write
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> hamQslBandRecurringTask(
            HamQslBandClient hamQslBandClient,
            BandConditionRepository repository,
            LoadingCache<String, List<BandCondition>> bandConditionsCache) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(hamQslBandClient, repository, bandConditionsCache));
    }

    /**
     * Executes the HamQSL band condition refresh.
     *
     * <p>Fetches data from API, saves to database, then triggers async cache refresh.
     * The cache refresh is non-blocking - old cached value is served during refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param hamQslBandClient    the HamQSL band API client
     * @param repository          the band condition repository
     * @param bandConditionsCache the cache to refresh
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(
            HamQslBandClient hamQslBandClient,
            BandConditionRepository repository,
            LoadingCache<String, List<BandCondition>> bandConditionsCache) {

        LOG.debug("Executing HamQSL band refresh task");

        try {
            // Fetch fresh data from API
            List<BandCondition> conditions = hamQslBandClient.fetch();

            // Convert to entities and save
            Instant now = Instant.now();
            List<BandConditionEntity> entities = conditions.stream()
                    .map(bc -> BandConditionEntity.fromDomain(bc, now))
                    .toList();

            repository.saveAll(entities);

            // Trigger async cache refresh (non-blocking)
            bandConditionsCache.refresh(CacheConfig.CACHE_KEY);

            LOG.info("HamQSL band refresh complete: {} band conditions saved, cache refresh triggered",
                    entities.size());

        } catch (Exception e) {
            LOG.error("HamQSL band refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("HamQSL band refresh failed", e);
        }
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the band condition repository
     * @return true if no recent band condition data exists
     */
    public boolean needsInitialLoad(BandConditionRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(30));
        return repository.findByRecordedAtAfterOrderByRecordedAtDesc(recent).isEmpty();
    }
}
