package io.nextskip.contests.api;

import io.nextskip.contests.model.Contest;

import java.util.List;

/**
 * Public API for contest calendar data.
 *
 * <p>This is the module's public contract - other modules should depend on this
 * interface rather than the internal implementation.
 */
public interface ContestService {

    /**
     * Get upcoming amateur radio contests.
     *
     * <p>Returns contests from the WA7BNM contest calendar feed, typically covering
     * the next 8 days. Contests are returned unsorted - callers should sort by
     * their preferred criteria (start time, score, etc.).
     *
     * @return List of upcoming contests, or empty list if unavailable
     */
    List<Contest> getUpcomingContests();
}
