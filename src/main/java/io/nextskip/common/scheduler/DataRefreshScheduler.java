package io.nextskip.common.scheduler;

import io.nextskip.common.client.RefreshableDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduler that refreshes all data sources on startup and at regular intervals.
 *
 * <p>This scheduler automatically discovers all {@link RefreshableDataSource} implementations
 * via Spring's dependency injection. Adding a new data source only requires implementing
 * the interface - no modifications to this class are needed (Open-Closed Principle).
 *
 * <p>Features:
 * <ul>
 *     <li>Eager loading: warms all caches on application startup</li>
 *     <li>Per-source intervals: each source defines its own refresh interval</li>
 *     <li>Fault tolerance: failed refreshes are logged, not propagated</li>
 *     <li>Clean shutdown: cancels all scheduled tasks on bean destruction</li>
 * </ul>
 */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: safeRefresh catches all exceptions to prevent scheduler crash
public class DataRefreshScheduler implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(DataRefreshScheduler.class);

    private final List<RefreshableDataSource> sources;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();

    @Value("${nextskip.refresh.eager-load:true}")
    private boolean eagerLoad;

    /**
     * Constructs a scheduler with the given data sources.
     *
     * @param sources all RefreshableDataSource beans (auto-discovered by Spring)
     */
    public DataRefreshScheduler(List<RefreshableDataSource> sources) {
        this.sources = List.copyOf(sources);  // Defensive copy
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(2);
        this.taskScheduler.setThreadNamePrefix("data-refresh-");
        this.taskScheduler.initialize();
    }

    /**
     * Initializes the scheduler after bean construction.
     *
     * <p>If eager loading is enabled (default), refreshes all sources immediately
     * to warm the caches. Then schedules each source to refresh at its specified interval.
     */
    @PostConstruct
    public void initialize() {
        if (sources.isEmpty()) {
            LOG.warn("No RefreshableDataSource beans found - nothing to schedule");
            return;
        }

        if (eagerLoad) {
            LOG.info("Warming cache on startup with {} sources...", sources.size());
            sources.forEach(this::safeRefresh);
            LOG.info("Cache warming complete");
        }

        // Stagger refresh timing to prevent concurrent memory spikes
        // Each source starts 10 seconds after the previous to spread load
        long staggerOffsetSeconds = 0;
        for (RefreshableDataSource source : sources) {
            // Use the refresh interval as both initial delay and period
            // Add stagger offset to prevent all sources refreshing simultaneously
            Instant firstRun = Instant.now()
                    .plus(source.getRefreshInterval())
                    .plusSeconds(staggerOffsetSeconds);
            ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                    () -> safeRefresh(source),
                    firstRun,
                    source.getRefreshInterval()
            );
            scheduledTasks.add(future);
            LOG.info("Scheduled {} to refresh every {} (offset: {}s)",
                    source.getSourceName(), source.getRefreshInterval(), staggerOffsetSeconds);
            staggerOffsetSeconds += 10;
        }
    }

    /**
     * Safely refreshes a data source, catching and logging any exceptions.
     *
     * @param source the source to refresh
     */
    private void safeRefresh(RefreshableDataSource source) {
        try {
            source.refresh();
            LOG.debug("Refreshed {}", source.getSourceName());
        } catch (Exception e) {
            LOG.warn("Failed to refresh {}: {}", source.getSourceName(), e.getMessage());
        }
    }

    @Override
    public void destroy() {
        LOG.info("Shutting down data refresh scheduler");
        scheduledTasks.forEach(future -> future.cancel(true));
        taskScheduler.shutdown();
    }
}
