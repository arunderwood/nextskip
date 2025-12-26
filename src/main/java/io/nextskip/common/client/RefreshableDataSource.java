package io.nextskip.common.client;

import java.time.Duration;

/**
 * Interface for data sources that can be refreshed on a schedule.
 *
 * <p>Implement this interface to have your data source automatically discovered
 * and scheduled by {@link io.nextskip.common.scheduler.DataRefreshScheduler}.
 *
 * <p>The scheduler will:
 * <ul>
 *     <li>Call {@link #refresh()} on startup to warm the cache</li>
 *     <li>Schedule periodic refreshes at the interval returned by {@link #getRefreshInterval()}</li>
 *     <li>Log refresh activity using {@link #getSourceName()} for identification</li>
 * </ul>
 *
 * <p>This enables the Open-Closed Principle: adding a new data source only requires
 * implementing this interface - no modifications to the scheduler are needed.
 */
public interface RefreshableDataSource {

    /**
     * Fetches fresh data from the source and updates the cache.
     *
     * <p>Implementations should delegate to their cacheable fetch method,
     * ensuring the cache is refreshed. Any exceptions should be allowed to
     * propagate - the scheduler will catch and log them.
     */
    void refresh();

    /**
     * Returns the human-readable name of this data source.
     *
     * <p>Used for logging refresh activity and identifying the source.
     *
     * @return the source name (e.g., "POTA", "SOTA", "NOAA SWPC")
     */
    String getSourceName();

    /**
     * Returns the recommended refresh interval for this data source.
     *
     * <p>The scheduler will refresh this source at this interval.
     * Choose an interval appropriate for the data's update frequency
     * and the external API's rate limits.
     *
     * @return the refresh interval (e.g., {@code Duration.ofMinutes(2)})
     */
    Duration getRefreshInterval();
}
