package io.nextskip.admin.api;

import com.vaadin.hilla.BrowserCallable;
import io.nextskip.common.admin.AdminStatusProvider;
import io.nextskip.common.admin.TriggerRefreshResult;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Hilla endpoint for admin feed management operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Getting status of all feeds across all modules</li>
 *   <li>Triggering manual refresh of individual feeds</li>
 * </ul>
 *
 * <p>All methods require ADMIN role. Feed status is aggregated from all
 * {@link AdminStatusProvider} implementations discovered in the application context.
 */
@BrowserCallable
@Service
@RolesAllowed("ADMIN")
public class AdminFeedEndpoint {

    private final List<AdminStatusProvider> statusProviders;

    /**
     * Creates a new AdminFeedEndpoint.
     *
     * <p>All AdminStatusProvider beans are automatically injected via Spring's
     * List injection, allowing new modules to add their providers without
     * modifying this class.
     *
     * @param statusProviders list of all AdminStatusProvider implementations
     */
    public AdminFeedEndpoint(List<AdminStatusProvider> statusProviders) {
        this.statusProviders = List.copyOf(statusProviders);
    }

    /**
     * Gets the current status of all feeds across all modules.
     *
     * <p>Aggregates status from all registered AdminStatusProvider implementations.
     * Each module provides its own feed statuses, which are grouped by module name.
     *
     * @return FeedStatusResponse containing all module feed statuses
     */
    public FeedStatusResponse getFeedStatuses() {
        List<ModuleFeedStatus> moduleStatuses = statusProviders.stream()
                .map(provider -> ModuleFeedStatus.of(
                        provider.getModuleName(),
                        provider.getFeedStatuses()
                ))
                .toList();

        return FeedStatusResponse.of(moduleStatuses);
    }

    /**
     * Triggers immediate refresh of a specific feed.
     *
     * <p>Searches all providers for a feed matching the given name and triggers
     * its refresh if found. Only scheduled feeds can be refreshed; subscription
     * feeds will return an appropriate error message.
     *
     * @param feedName the name of the feed to refresh (e.g., "NOAA SWPC", "POTA")
     * @return TriggerRefreshResult indicating success or failure
     */
    public TriggerRefreshResult triggerRefresh(String feedName) {
        // Search all providers for the feed
        for (AdminStatusProvider provider : statusProviders) {
            Optional<TriggerRefreshResult> result = provider.triggerRefresh(feedName);
            if (result.isPresent()) {
                return result.get();
            }
        }

        // Feed not found in any provider
        return TriggerRefreshResult.unknownFeed(feedName);
    }
}
