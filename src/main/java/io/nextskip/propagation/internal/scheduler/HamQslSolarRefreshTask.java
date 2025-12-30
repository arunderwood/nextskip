package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.HamQslSolarClient;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task for refreshing HamQSL solar indices data.
 *
 * <p>Fetches solar indices (SFI, K-index, A-index, sunspots) from HamQSL,
 * persists them to the database, and triggers an async cache refresh.
 * HamQSL provides K and A indices which complement NOAA's SFI/sunspots.
 *
 * <p>Task runs every 30 minutes (matching the HamQSL client refresh interval).
 */
@Configuration
public class HamQslSolarRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(HamQslSolarRefreshTask.class);
    private static final String TASK_NAME = "hamqsl-solar-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(30);
    private static final String HAMQSL_SOURCE = "HamQSL";

    /**
     * Creates the recurring task bean for HamQSL solar data refresh.
     *
     * @param hamQslSolarClient the HamQSL solar API client
     * @param repository        the solar indices repository for persistence
     * @param solarIndicesCache the LoadingCache to refresh after DB write
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> hamQslSolarRecurringTask(
            HamQslSolarClient hamQslSolarClient,
            SolarIndicesRepository repository,
            LoadingCache<String, SolarIndices> solarIndicesCache) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(hamQslSolarClient, repository, solarIndicesCache));
    }

    /**
     * Executes the HamQSL solar data refresh.
     *
     * <p>Fetches data from API, saves to database, then triggers async cache refresh.
     * The cache refresh is non-blocking - old cached value is served during refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param hamQslSolarClient the HamQSL solar API client
     * @param repository        the solar indices repository
     * @param solarIndicesCache the cache to refresh
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(
            HamQslSolarClient hamQslSolarClient,
            SolarIndicesRepository repository,
            LoadingCache<String, SolarIndices> solarIndicesCache) {

        LOG.debug("Executing HamQSL solar refresh task");

        try {
            // Fetch fresh data from API
            SolarIndices indices = hamQslSolarClient.fetch();

            if (indices != null) {
                // Convert to entity and save
                SolarIndicesEntity entity = SolarIndicesEntity.fromDomain(indices);
                repository.save(entity);

                // Trigger async cache refresh (non-blocking)
                solarIndicesCache.refresh(CacheConfig.CACHE_KEY);

                LOG.info("HamQSL solar refresh complete: K={}, A={}, cache refresh triggered",
                        indices.kIndex(), indices.aIndex());
            } else {
                LOG.warn("HamQSL solar client returned null - skipping save");
            }

        } catch (Exception e) {
            LOG.error("HamQSL solar refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("HamQSL solar refresh failed", e);
        }
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the solar indices repository
     * @return true if no recent HamQSL data exists
     */
    public boolean needsInitialLoad(SolarIndicesRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(60));
        return repository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .noneMatch(e -> HAMQSL_SOURCE.equals(e.getSource()));
    }
}
