package io.nextskip.contests.api;

import io.nextskip.contests.model.Contest;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for contest data exposed to frontend.
 *
 * <p>Contains upcoming contest information for dashboard display.
 *
 * @param contests List of upcoming contests (typically 8-day window from WA7BNM)
 * @param activeCount Number of contests currently active
 * @param upcomingCount Number of contests starting soon (within 24 hours)
 * @param totalCount Total number of contests in the list
 * @param lastUpdated Timestamp when this data was generated
 */
public record ContestsResponse(
        List<Contest> contests,
        int activeCount,
        int upcomingCount,
        int totalCount,
        Instant lastUpdated
) {
}
