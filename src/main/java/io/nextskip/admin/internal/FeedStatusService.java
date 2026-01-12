package io.nextskip.admin.internal;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.admin.model.FeedStatus;
import io.nextskip.common.api.SubscriptionStatusProvider;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Aggregates feed status from all data sources for the admin UI.
 *
 * <p>Automatically discovers feeds through Spring dependency injection:
 * <ul>
 *   <li>{@link RefreshTaskCoordinator} - scheduled feeds (db-scheduler)</li>
 *   <li>{@link SubscriptionStatusProvider} - subscription feeds (MQTT, etc.)</li>
 * </ul>
 *
 * <p>Follows the Open-Closed Principle: adding new feeds requires only implementing
 * the appropriate interface, with no changes to this service.
 */
@Service
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
        justification = "Spring-managed singleton; constructor throws are caught by container")
public class FeedStatusService {

    private static final Logger LOG = LoggerFactory.getLogger(FeedStatusService.class);

    /**
     * Maximum time since last execution before a scheduled feed is considered unhealthy.
     * Set to 2 hours to accommodate feeds with 1-hour intervals plus buffer.
     */
    private static final Duration SCHEDULED_STALE_THRESHOLD = Duration.ofHours(2);

    @Nullable
    private final Scheduler scheduler;
    private final List<RefreshTaskCoordinator> scheduledFeeds;
    private final List<SubscriptionStatusProvider> subscriptionFeeds;

    /**
     * Creates a new FeedStatusService.
     *
     * <p>Spring automatically injects all beans implementing the coordinator interfaces.
     * The scheduler is optional and may be null when db-scheduler is disabled (e.g., in tests).
     *
     * @param schedulerProvider the db-scheduler Scheduler provider (optional, may provide null)
     * @param scheduledFeeds all registered RefreshTaskCoordinators
     * @param subscriptionFeeds all registered SubscriptionStatusProviders
     */
    public FeedStatusService(
            ObjectProvider<Scheduler> schedulerProvider,
            List<RefreshTaskCoordinator> scheduledFeeds,
            List<SubscriptionStatusProvider> subscriptionFeeds) {
        this.scheduler = schedulerProvider.getIfAvailable();
        this.scheduledFeeds = List.copyOf(scheduledFeeds);
        this.subscriptionFeeds = List.copyOf(subscriptionFeeds);
        if (scheduler == null) {
            LOG.info("FeedStatusService initialized without scheduler (disabled). "
                    + "Scheduled feed status will show as unavailable.");
        } else {
            LOG.info("FeedStatusService initialized with {} scheduled feeds and {} subscription feeds",
                    this.scheduledFeeds.size(), this.subscriptionFeeds.size());
        }
    }

    /**
     * Returns the status of all feeds, sorted alphabetically by display name.
     *
     * @return list of all feed statuses
     */
    public List<FeedStatus> getAllFeedStatuses() {
        List<FeedStatus> statuses = new ArrayList<>();

        // Collect scheduled feed statuses
        for (RefreshTaskCoordinator coordinator : scheduledFeeds) {
            statuses.add(getScheduledFeedStatus(coordinator));
        }

        // Collect subscription feed statuses
        for (SubscriptionStatusProvider provider : subscriptionFeeds) {
            statuses.add(getSubscriptionFeedStatus(provider));
        }

        // Sort alphabetically by display name
        statuses.sort(Comparator.comparing(FeedStatus::displayName));

        return statuses;
    }

    /**
     * Returns the status of a single feed by ID.
     *
     * @param feedId the feed identifier
     * @return the feed status, or empty if not found
     */
    public Optional<FeedStatus> getFeedStatus(String feedId) {
        // Check scheduled feeds
        for (RefreshTaskCoordinator coordinator : scheduledFeeds) {
            if (coordinator.getTaskName().equals(feedId)) {
                return Optional.of(getScheduledFeedStatus(coordinator));
            }
        }

        // Check subscription feeds
        for (SubscriptionStatusProvider provider : subscriptionFeeds) {
            if (provider.getSubscriptionId().equals(feedId)) {
                return Optional.of(getSubscriptionFeedStatus(provider));
            }
        }

        return Optional.empty();
    }

    /**
     * Triggers an immediate refresh of a scheduled feed.
     *
     * @param feedId the feed identifier (task name)
     * @return true if the refresh was scheduled, false if feed not found or not schedulable
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Scheduler can throw various runtime exceptions
    public boolean triggerRefresh(String feedId) {
        if (scheduler == null) {
            LOG.warn("Cannot trigger refresh - scheduler is disabled");
            return false;
        }

        for (RefreshTaskCoordinator coordinator : scheduledFeeds) {
            if (coordinator.getTaskName().equals(feedId)) {
                try {
                    scheduler.reschedule(
                            coordinator.getRecurringTask().instance(RecurringTask.INSTANCE),
                            Instant.now()
                    );
                    LOG.info("Triggered manual refresh for feed: {}", feedId);
                    return true;
                } catch (Exception e) {
                    LOG.error("Failed to trigger refresh for feed {}: {}", feedId, e.getMessage());
                    return false;
                }
            }
        }

        LOG.warn("Feed not found or not refreshable: {}", feedId);
        return false;
    }

    /**
     * Checks if a feed ID corresponds to a scheduled (refreshable) feed.
     *
     * @param feedId the feed identifier
     * @return true if the feed is scheduled and can be manually refreshed
     */
    public boolean isScheduledFeed(String feedId) {
        return scheduledFeeds.stream()
                .anyMatch(c -> c.getTaskName().equals(feedId));
    }

    private FeedStatus getScheduledFeedStatus(RefreshTaskCoordinator coordinator) {
        String taskName = coordinator.getTaskName();
        String displayName = coordinator.getDisplayName();

        // Handle case where scheduler is disabled (e.g., in tests)
        if (scheduler == null) {
            return FeedStatus.scheduledUnhealthy(taskName, displayName, null, "Scheduler disabled");
        }

        TaskInstanceId instanceId = TaskInstanceId.of(taskName, RecurringTask.INSTANCE);
        Optional<ScheduledExecution<Object>> execution = scheduler.getScheduledExecution(instanceId);

        if (execution.isEmpty()) {
            return FeedStatus.scheduledUnhealthy(taskName, displayName, null, "Not scheduled");
        }

        ScheduledExecution<Object> exec = execution.get();
        Instant lastSuccess = exec.getLastSuccess();

        // Check if last execution was recent enough
        if (lastSuccess == null) {
            return FeedStatus.scheduledUnhealthy(taskName, displayName, null, "Never executed");
        }

        Duration sinceLastRun = Duration.between(lastSuccess, Instant.now());
        if (sinceLastRun.compareTo(SCHEDULED_STALE_THRESHOLD) > 0) {
            return FeedStatus.scheduledUnhealthy(taskName, displayName, lastSuccess,
                    "Stale - last run " + formatDuration(sinceLastRun) + " ago");
        }

        return FeedStatus.scheduledHealthy(taskName, displayName, lastSuccess);
    }

    private FeedStatus getSubscriptionFeedStatus(SubscriptionStatusProvider provider) {
        String id = provider.getSubscriptionId();
        String displayName = provider.getDisplayName();
        boolean connected = provider.isConnected();
        Instant lastMessage = provider.getLastMessageTime();

        if (connected) {
            return FeedStatus.subscriptionConnected(id, displayName, lastMessage);
        } else {
            return FeedStatus.subscriptionDisconnected(id, displayName, lastMessage);
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + "h";
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + "m";
        }
        return duration.toSeconds() + "s";
    }
}
