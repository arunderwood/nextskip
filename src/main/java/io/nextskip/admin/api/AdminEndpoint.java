package io.nextskip.admin.api;

import com.vaadin.hilla.BrowserCallable;
import io.nextskip.admin.internal.FeedStatusService;
import io.nextskip.admin.model.FeedStatus;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/**
 * Hilla endpoint for admin feed management operations.
 *
 * <p>Provides browser-callable methods for the admin Feed Manager UI.
 * All methods require ADMIN role, enforced by Spring Security.
 */
@BrowserCallable
@RolesAllowed("ADMIN")
public class AdminEndpoint {

    private final FeedStatusService feedStatusService;

    /**
     * Creates a new AdminEndpoint.
     *
     * @param feedStatusService the service for feed status operations
     */
    public AdminEndpoint(FeedStatusService feedStatusService) {
        this.feedStatusService = feedStatusService;
    }

    /**
     * Returns the status of all data feeds.
     *
     * <p>Includes both scheduled feeds (db-scheduler tasks) and
     * subscription feeds (MQTT connections).
     *
     * @return list of all feed statuses, sorted alphabetically by display name
     */
    public List<FeedStatus> getFeedStatuses() {
        return feedStatusService.getAllFeedStatuses();
    }

    /**
     * Triggers an immediate refresh of a scheduled feed.
     *
     * <p>Only works for scheduled feeds. Subscription feeds cannot be
     * manually refreshed (they maintain persistent connections).
     *
     * @param feedId the feed identifier (task name)
     * @return true if refresh was triggered, false if feed not found or not refreshable
     */
    public boolean triggerRefresh(String feedId) {
        return feedStatusService.triggerRefresh(feedId);
    }

    /**
     * Checks if a feed can be manually refreshed.
     *
     * @param feedId the feed identifier
     * @return true if the feed is a scheduled feed that can be refreshed
     */
    public boolean canRefresh(String feedId) {
        return feedStatusService.isScheduledFeed(feedId);
    }
}
