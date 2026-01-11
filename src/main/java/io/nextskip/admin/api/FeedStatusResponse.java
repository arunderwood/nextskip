package io.nextskip.admin.api;

import java.time.Instant;
import java.util.List;

/**
 * Response containing feed status for all modules.
 *
 * <p>Top-level response object returned by {@link AdminFeedEndpoint#getFeedStatuses()}.
 *
 * @param modules List of module feed statuses
 * @param timestamp When this status snapshot was taken
 * @param totalFeeds Total number of feeds across all modules
 */
public record FeedStatusResponse(
        List<ModuleFeedStatus> modules,
        Instant timestamp,
        int totalFeeds
) {

    /**
     * Canonical constructor that creates defensive copy of modules list.
     */
    public FeedStatusResponse {
        modules = List.copyOf(modules);
    }

    /**
     * Creates a FeedStatusResponse from a list of module statuses.
     *
     * @param modules List of module feed statuses
     * @return FeedStatusResponse with calculated totals
     */
    public static FeedStatusResponse of(List<ModuleFeedStatus> modules) {
        int total = modules.stream()
                .mapToInt(ModuleFeedStatus::totalFeeds)
                .sum();

        return new FeedStatusResponse(modules, Instant.now(), total);
    }
}
