package io.nextskip.activations.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.activations.internal.PotaClient;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
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
 * Recurring task for refreshing POTA activation data.
 *
 * <p>Fetches current POTA spots from the API and persists them to the database.
 * Old data is automatically cleaned up to prevent unbounded growth.
 *
 * <p>Task runs every 1 minute (matching the original refresh interval).
 */
@Configuration
public class PotaRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(PotaRefreshTask.class);
    private static final String TASK_NAME = "pota-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration DATA_RETENTION = Duration.ofHours(2);

    /**
     * Creates the recurring task bean for POTA data refresh.
     *
     * @param potaClient the POTA API client
     * @param repository the activation repository for persistence
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> potaRecurringTask(
            PotaClient potaClient,
            ActivationRepository repository) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(potaClient, repository));
    }

    /**
     * Executes the POTA data refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param potaClient the POTA API client
     * @param repository the activation repository
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(PotaClient potaClient, ActivationRepository repository) {
        LOG.debug("Executing POTA refresh task");

        try {
            // Fetch fresh data from API
            List<Activation> activations = potaClient.fetch();

            // Convert to entities and save
            List<ActivationEntity> entities = activations.stream()
                    .map(ActivationEntity::fromDomain)
                    .toList();

            repository.saveAll(entities);

            // Cleanup old data
            Instant cutoff = Instant.now().minus(DATA_RETENTION);
            int deleted = repository.deleteBySpottedAtBefore(cutoff);

            LOG.info("POTA refresh complete: {} activations saved, {} old records deleted",
                    entities.size(), deleted);

        } catch (Exception e) {
            LOG.error("POTA refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("POTA refresh failed", e);
        }
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the activation repository
     * @return true if no recent POTA data exists
     */
    public boolean needsInitialLoad(ActivationRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(5));
        return repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                ActivationType.POTA, recent).isEmpty();
    }
}
