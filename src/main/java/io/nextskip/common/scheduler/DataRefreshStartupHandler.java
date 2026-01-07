package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Handles conditional startup execution of data refresh tasks.
 *
 * <p>On application startup, checks each data repository for recent data.
 * If a repository is empty (cold start), the corresponding refresh task
 * is rescheduled for immediate execution.
 *
 * <p>This ensures the application has fresh data available immediately
 * after a cold start (e.g., new deployment, database migration), while
 * avoiding redundant API calls when data is already present (e.g., restart).
 *
 * <p>Uses the {@link RefreshTaskCoordinator} interface to automatically discover
 * and coordinate all refresh tasks. New tasks are automatically included without
 * modifying this class (Open-Closed Principle).
 */
@Component
@ConditionalOnProperty(value = "db-scheduler.enabled", havingValue = "true")
public class DataRefreshStartupHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DataRefreshStartupHandler.class);

    private final Scheduler scheduler;
    private final boolean eagerLoadEnabled;
    private final List<RefreshTaskCoordinator> coordinators;

    /**
     * Creates a new startup handler.
     *
     * <p>Coordinators are automatically discovered via Spring's component scanning.
     * Each coordinator encapsulates its task, repository check, and task name.
     *
     * @param scheduler        the db-scheduler Scheduler
     * @param eagerLoadEnabled whether eager loading is enabled
     * @param coordinators     all registered refresh task coordinators
     */
    public DataRefreshStartupHandler(
            Scheduler scheduler,
            @Value("${nextskip.refresh.eager-load:true}") boolean eagerLoadEnabled,
            List<RefreshTaskCoordinator> coordinators) {

        this.scheduler = scheduler;
        this.eagerLoadEnabled = eagerLoadEnabled;
        this.coordinators = List.copyOf(coordinators);
    }

    /**
     * Handles the ApplicationReadyEvent to check for empty repositories.
     *
     * <p>For each coordinator, checks if initial data load is needed.
     * If so, reschedules the corresponding task for immediate execution.
     *
     * @param event the application ready event
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!eagerLoadEnabled) {
            LOG.info("Eager loading disabled - skipping startup data refresh checks");
            return;
        }

        LOG.info("Checking for required startup data refreshes ({} coordinators)...",
                coordinators.size());

        int tasksScheduled = 0;

        for (RefreshTaskCoordinator coordinator : coordinators) {
            if (coordinator.needsInitialLoad()) {
                rescheduleForImmediateExecution(
                        coordinator.getRecurringTask(),
                        coordinator.getTaskName());
                tasksScheduled++;
            }
        }

        if (tasksScheduled > 0) {
            LOG.info("Scheduled {} tasks for immediate execution due to empty data", tasksScheduled);
        } else {
            LOG.info("All data repositories have recent data - no immediate refresh needed");
        }
    }

    /**
     * Reschedules a recurring task for immediate execution.
     *
     * @param task     the recurring task to reschedule
     * @param taskName friendly name for logging
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Scheduler can throw various runtime exceptions
    private void rescheduleForImmediateExecution(RecurringTask<Void> task, String taskName) {
        LOG.info("Scheduling immediate {} data refresh - database appears empty", taskName);
        try {
            scheduler.reschedule(
                    task.instance(RecurringTask.INSTANCE),
                    Instant.now()
            );
        } catch (Exception e) {
            LOG.warn("Failed to reschedule {} task for immediate execution: {}",
                    taskName, e.getMessage());
        }
    }
}
