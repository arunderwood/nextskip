package io.nextskip.admin.api;

import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.SubscriptionFeedStatus;

import java.util.List;

/**
 * Feed status information for a single module.
 *
 * <p>Groups feed statuses by module for easier consumption by the admin UI.
 *
 * @param moduleName The module name (e.g., "propagation", "activations")
 * @param scheduledFeeds Status of scheduled/polling feeds in this module
 * @param subscriptionFeeds Status of subscription/streaming feeds in this module
 */
public record ModuleFeedStatus(
        String moduleName,
        List<ScheduledFeedStatus> scheduledFeeds,
        List<SubscriptionFeedStatus> subscriptionFeeds
) {

    /**
     * Canonical constructor that creates defensive copies of lists.
     */
    public ModuleFeedStatus {
        scheduledFeeds = List.copyOf(scheduledFeeds);
        subscriptionFeeds = List.copyOf(subscriptionFeeds);
    }

    /**
     * Creates a ModuleFeedStatus from a module name and list of feed statuses.
     *
     * <p>Automatically separates scheduled and subscription feeds based on type.
     *
     * @param moduleName The module name
     * @param feedStatuses All feed statuses for the module
     * @return ModuleFeedStatus with feeds categorized by type
     */
    public static ModuleFeedStatus of(String moduleName, List<FeedStatus> feedStatuses) {
        List<ScheduledFeedStatus> scheduled = feedStatuses.stream()
                .filter(ScheduledFeedStatus.class::isInstance)
                .map(ScheduledFeedStatus.class::cast)
                .toList();

        List<SubscriptionFeedStatus> subscription = feedStatuses.stream()
                .filter(SubscriptionFeedStatus.class::isInstance)
                .map(SubscriptionFeedStatus.class::cast)
                .toList();

        return new ModuleFeedStatus(moduleName, scheduled, subscription);
    }

    /**
     * Returns total number of feeds in this module.
     *
     * @return total feed count
     */
    public int totalFeeds() {
        return scheduledFeeds.size() + subscriptionFeeds.size();
    }
}
