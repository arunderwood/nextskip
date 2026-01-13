package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;

/**
 * Unified recurring task for refreshing HamQSL solar and band condition data.
 *
 * <p>This task consolidates the previous separate tasks ({@code HamQslSolarRefreshTask}
 * and {@code HamQslBandRefreshTask}) to delegate to the unified
 * {@link HamQslRefreshService} which performs a single HTTP fetch for both data types.
 *
 * <p>Task runs every 2 hours (matching the HamQSL source update frequency of ~3 hours,
 * with a margin for freshness).
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
public class HamQslRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "hamqsl-refresh";
    private static final String DISPLAY_NAME = "HamQSL";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(2);
    private static final String HAMQSL_SOURCE = "HamQSL";
    private static final Duration STALE_THRESHOLD = Duration.ofHours(3);

    private final SolarIndicesRepository solarRepository;
    private final BandConditionRepository bandRepository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new HamQSL refresh task coordinator.
     *
     * @param solarRepository the solar indices repository
     * @param bandRepository the band condition repository
     */
    public HamQslRefreshTask(
            SolarIndicesRepository solarRepository,
            BandConditionRepository bandRepository) {
        this.solarRepository = solarRepository;
        this.bandRepository = bandRepository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * @param recurringTask the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(
            @Lazy @Qualifier("hamQslRecurringTask") RecurringTask<Void> task) {
        this.recurringTask = task;
    }

    /**
     * Creates the recurring task bean for unified HamQSL data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> hamQslRecurringTask(HamQslRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    @Override
    public RecurringTask<Void> getRecurringTask() {
        return recurringTask;
    }

    @Override
    public boolean needsInitialLoad() {
        Instant recent = Instant.now().minus(STALE_THRESHOLD);

        // Check if we have recent solar indices from HamQSL
        boolean hasSolarData = solarRepository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .anyMatch(e -> HAMQSL_SOURCE.equals(e.getSource()));

        // Check if we have recent band conditions
        boolean hasBandData = !bandRepository.findByRecordedAtAfterOrderByRecordedAtDesc(recent).isEmpty();

        // Need initial load if either is missing
        return !hasSolarData || !hasBandData;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}
