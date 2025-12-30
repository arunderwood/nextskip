package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.activations.internal.scheduler.PotaRefreshTask;
import io.nextskip.activations.internal.scheduler.SotaRefreshTask;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.contests.internal.scheduler.ContestRefreshTask;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.meteors.internal.scheduler.MeteorRefreshTask;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import io.nextskip.propagation.internal.scheduler.HamQslBandRefreshTask;
import io.nextskip.propagation.internal.scheduler.HamQslSolarRefreshTask;
import io.nextskip.propagation.internal.scheduler.NoaaRefreshTask;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
 */
@Component
@ConditionalOnBean(Scheduler.class)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class DataRefreshStartupHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DataRefreshStartupHandler.class);

    private final Scheduler scheduler;
    private final boolean eagerLoadEnabled;

    // Task classes for initial load checks
    private final PotaRefreshTask potaRefreshTask;
    private final SotaRefreshTask sotaRefreshTask;
    private final NoaaRefreshTask noaaRefreshTask;
    private final HamQslSolarRefreshTask hamQslSolarRefreshTask;
    private final HamQslBandRefreshTask hamQslBandRefreshTask;
    private final ContestRefreshTask contestRefreshTask;
    private final MeteorRefreshTask meteorRefreshTask;

    // Repositories for checking data presence
    private final ActivationRepository activationRepository;
    private final SolarIndicesRepository solarIndicesRepository;
    private final BandConditionRepository bandConditionRepository;
    private final ContestRepository contestRepository;
    private final MeteorShowerRepository meteorShowerRepository;

    // Task beans for rescheduling
    private final RecurringTask<Void> potaTask;
    private final RecurringTask<Void> sotaTask;
    private final RecurringTask<Void> noaaTask;
    private final RecurringTask<Void> hamQslSolarTask;
    private final RecurringTask<Void> hamQslBandTask;
    private final RecurringTask<Void> contestTask;
    private final RecurringTask<Void> meteorTask;

    /**
     * Creates a new startup handler.
     *
     * @param scheduler                the db-scheduler Scheduler
     * @param eagerLoadEnabled         whether eager loading is enabled
     * @param potaRefreshTask          POTA task class
     * @param sotaRefreshTask          SOTA task class
     * @param noaaRefreshTask          NOAA task class
     * @param hamQslSolarRefreshTask   HamQSL solar task class
     * @param hamQslBandRefreshTask    HamQSL band task class
     * @param contestRefreshTask       Contest task class
     * @param meteorRefreshTask        Meteor task class
     * @param activationRepository     Activation repository
     * @param solarIndicesRepository   Solar indices repository
     * @param bandConditionRepository  Band condition repository
     * @param contestRepository        Contest repository
     * @param meteorShowerRepository   Meteor shower repository
     * @param potaRecurringTask        POTA recurring task bean
     * @param sotaRecurringTask        SOTA recurring task bean
     * @param noaaRecurringTask        NOAA recurring task bean
     * @param hamQslSolarRecurringTask HamQSL solar recurring task bean
     * @param hamQslBandRecurringTask  HamQSL band recurring task bean
     * @param contestRecurringTask     Contest recurring task bean
     * @param meteorRecurringTask      Meteor recurring task bean
     */
    @SuppressWarnings("java:S107") // Required for Spring DI - many tasks to coordinate
    public DataRefreshStartupHandler(
            Scheduler scheduler,
            @Value("${nextskip.refresh.eager-load:true}") boolean eagerLoadEnabled,
            PotaRefreshTask potaRefreshTask,
            SotaRefreshTask sotaRefreshTask,
            NoaaRefreshTask noaaRefreshTask,
            HamQslSolarRefreshTask hamQslSolarRefreshTask,
            HamQslBandRefreshTask hamQslBandRefreshTask,
            ContestRefreshTask contestRefreshTask,
            MeteorRefreshTask meteorRefreshTask,
            ActivationRepository activationRepository,
            SolarIndicesRepository solarIndicesRepository,
            BandConditionRepository bandConditionRepository,
            ContestRepository contestRepository,
            MeteorShowerRepository meteorShowerRepository,
            RecurringTask<Void> potaRecurringTask,
            RecurringTask<Void> sotaRecurringTask,
            RecurringTask<Void> noaaRecurringTask,
            RecurringTask<Void> hamQslSolarRecurringTask,
            RecurringTask<Void> hamQslBandRecurringTask,
            RecurringTask<Void> contestRecurringTask,
            RecurringTask<Void> meteorRecurringTask) {

        this.scheduler = scheduler;
        this.eagerLoadEnabled = eagerLoadEnabled;
        this.potaRefreshTask = potaRefreshTask;
        this.sotaRefreshTask = sotaRefreshTask;
        this.noaaRefreshTask = noaaRefreshTask;
        this.hamQslSolarRefreshTask = hamQslSolarRefreshTask;
        this.hamQslBandRefreshTask = hamQslBandRefreshTask;
        this.contestRefreshTask = contestRefreshTask;
        this.meteorRefreshTask = meteorRefreshTask;
        this.activationRepository = activationRepository;
        this.solarIndicesRepository = solarIndicesRepository;
        this.bandConditionRepository = bandConditionRepository;
        this.contestRepository = contestRepository;
        this.meteorShowerRepository = meteorShowerRepository;
        this.potaTask = potaRecurringTask;
        this.sotaTask = sotaRecurringTask;
        this.noaaTask = noaaRecurringTask;
        this.hamQslSolarTask = hamQslSolarRecurringTask;
        this.hamQslBandTask = hamQslBandRecurringTask;
        this.contestTask = contestRecurringTask;
        this.meteorTask = meteorRecurringTask;
    }

    /**
     * Handles the ApplicationReadyEvent to check for empty repositories.
     *
     * <p>For each data type, checks if recent data exists. If not, reschedules
     * the corresponding task for immediate execution.
     *
     * @param event the application ready event
     */
    @EventListener
    @SuppressWarnings("PMD.NPathComplexity") // Sequential checks for each data source - intentionally explicit
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!eagerLoadEnabled) {
            LOG.info("Eager loading disabled - skipping startup data refresh checks");
            return;
        }

        LOG.info("Checking for required startup data refreshes...");

        int tasksScheduled = 0;

        // Check activations
        if (potaRefreshTask.needsInitialLoad(activationRepository)) {
            rescheduleForImmediateExecution(potaTask, "POTA");
            tasksScheduled++;
        }
        if (sotaRefreshTask.needsInitialLoad(activationRepository)) {
            rescheduleForImmediateExecution(sotaTask, "SOTA");
            tasksScheduled++;
        }

        // Check solar indices
        if (noaaRefreshTask.needsInitialLoad(solarIndicesRepository)) {
            rescheduleForImmediateExecution(noaaTask, "NOAA");
            tasksScheduled++;
        }
        if (hamQslSolarRefreshTask.needsInitialLoad(solarIndicesRepository)) {
            rescheduleForImmediateExecution(hamQslSolarTask, "HamQSL Solar");
            tasksScheduled++;
        }

        // Check band conditions
        if (hamQslBandRefreshTask.needsInitialLoad(bandConditionRepository)) {
            rescheduleForImmediateExecution(hamQslBandTask, "HamQSL Band");
            tasksScheduled++;
        }

        // Check contests
        if (contestRefreshTask.needsInitialLoad(contestRepository)) {
            rescheduleForImmediateExecution(contestTask, "Contest");
            tasksScheduled++;
        }

        // Check meteor showers
        if (meteorRefreshTask.needsInitialLoad(meteorShowerRepository)) {
            rescheduleForImmediateExecution(meteorTask, "Meteor");
            tasksScheduled++;
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
