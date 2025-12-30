package io.nextskip.meteors.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.meteors.internal.MeteorShowerDataLoader;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Recurring task for refreshing meteor shower data.
 *
 * <p>Loads meteor shower data from the curated JSON file, computes concrete
 * dates for the current and upcoming year, persists them to the database,
 * and triggers async cache refresh.
 *
 * <p>Task runs every 1 hour (shower dates are static, but allows for
 * year-end transitions and data file updates).
 */
@Configuration
public class MeteorRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorRefreshTask.class);
    private static final String TASK_NAME = "meteor-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);
    private static final int LOOKAHEAD_DAYS = 30;

    /**
     * Creates the recurring task bean for meteor shower data refresh.
     *
     * @param dataLoader         the meteor shower data loader
     * @param repository         the meteor shower repository for persistence
     * @param meteorShowersCache the LoadingCache to refresh after DB write
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> meteorRecurringTask(
            MeteorShowerDataLoader dataLoader,
            MeteorShowerRepository repository,
            LoadingCache<String, List<MeteorShower>> meteorShowersCache) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(dataLoader, repository, meteorShowersCache));
    }

    /**
     * Executes the meteor shower data refresh.
     *
     * <p>Loads data from JSON file, saves to database, then triggers async cache refresh.
     * The cache refresh is non-blocking - old cached value is served during refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param dataLoader         the meteor shower data loader
     * @param repository         the meteor shower repository
     * @param meteorShowersCache the cache to refresh
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Data loader can throw various exceptions
    void executeRefresh(
            MeteorShowerDataLoader dataLoader,
            MeteorShowerRepository repository,
            LoadingCache<String, List<MeteorShower>> meteorShowersCache) {

        LOG.debug("Executing meteor refresh task");

        try {
            // Load shower data with computed dates for current period
            List<MeteorShower> showers = dataLoader.getShowers(LOOKAHEAD_DAYS);

            // Convert to entities
            List<MeteorShowerEntity> entities = showers.stream()
                    .map(MeteorShowerEntity::fromDomain)
                    .toList();

            // Clear old data and save new (shower dates change annually)
            repository.deleteAll();
            repository.saveAll(entities);

            // Trigger async cache refresh (non-blocking)
            meteorShowersCache.refresh(CacheConfig.CACHE_KEY);

            LOG.info("Meteor refresh complete: {} showers saved, cache refresh triggered",
                    entities.size());

        } catch (Exception e) {
            LOG.error("Meteor refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("Meteor refresh failed", e);
        }
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the meteor shower repository
     * @return true if no meteor shower data exists
     */
    public boolean needsInitialLoad(MeteorShowerRepository repository) {
        // Check if we have any showers with visibility ending in the future
        Instant now = Instant.now();
        return repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                now, now).isEmpty()
                && repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(now).isEmpty();
    }
}
