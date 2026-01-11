package io.nextskip.common.admin;

import java.util.List;
import java.util.Optional;

/**
 * Interface for modules to expose their feed status to the admin UI.
 *
 * <p>Follows the same discovery pattern as {@link io.nextskip.common.scheduler.RefreshTaskCoordinator}.
 * Each activity module (propagation, activations, etc.) implements this interface to expose
 * its own feed status. The admin module aggregates all implementations to provide a complete view.
 *
 * <p>This design follows the Open-Closed Principle: adding a new module with feeds
 * automatically adds its status to the admin UI without modifying the admin module.
 *
 * @see io.nextskip.common.scheduler.RefreshTaskCoordinator
 */
public interface AdminStatusProvider {

    /**
     * Returns status of all feeds managed by this module.
     *
     * <p>Should include both scheduled feeds (with refresh times and failure counts)
     * and subscription feeds (with connection state) as appropriate for the module.
     *
     * @return list of feed statuses, never null
     */
    List<FeedStatus> getFeedStatuses();

    /**
     * Triggers immediate refresh of a scheduled feed.
     *
     * <p>Only applicable to scheduled feeds. For subscription feeds,
     * this should return {@link TriggerRefreshResult#notScheduledFeed(String)}.
     *
     * @param feedName the feed identifier (matches the name from {@link FeedStatus#name()})
     * @return result of the trigger attempt, or empty if feed not found in this module
     */
    Optional<TriggerRefreshResult> triggerRefresh(String feedName);

    /**
     * Returns the module name for grouping in the admin UI.
     *
     * <p>Examples: "propagation", "activations", "spots"
     *
     * @return module name, never null
     */
    String getModuleName();
}
