package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.NoaaSwpcClient;
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
 * Recurring task for refreshing NOAA SWPC solar indices data.
 *
 * <p>Fetches current solar indices (SFI, sunspot number) from NOAA's
 * Space Weather Prediction Center and persists them to the database.
 *
 * <p>Task runs every 5 minutes (matching the NOAA client refresh interval).
 */
@Configuration
public class NoaaRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(NoaaRefreshTask.class);
    private static final String TASK_NAME = "noaa-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final String NOAA_SOURCE = "NOAA";

    /**
     * Creates the recurring task bean for NOAA data refresh.
     *
     * @param noaaClient the NOAA SWPC API client
     * @param repository the solar indices repository for persistence
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> noaaRecurringTask(
            NoaaSwpcClient noaaClient,
            SolarIndicesRepository repository) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(noaaClient, repository));
    }

    /**
     * Executes the NOAA data refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param noaaClient the NOAA API client
     * @param repository the solar indices repository
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(NoaaSwpcClient noaaClient, SolarIndicesRepository repository) {
        LOG.debug("Executing NOAA refresh task");

        try {
            // Fetch fresh data from API
            SolarIndices indices = noaaClient.fetch();

            // Convert to entity and save
            SolarIndicesEntity entity = SolarIndicesEntity.fromDomain(indices);
            repository.save(entity);

            LOG.info("NOAA refresh complete: SFI={}, Sunspots={}",
                    indices.solarFluxIndex(), indices.sunspotNumber());

        } catch (Exception e) {
            LOG.error("NOAA refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("NOAA refresh failed", e);
        }
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the solar indices repository
     * @return true if no recent NOAA data exists
     */
    public boolean needsInitialLoad(SolarIndicesRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(10));
        return repository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .noneMatch(e -> NOAA_SOURCE.equals(e.getSource()));
    }
}
