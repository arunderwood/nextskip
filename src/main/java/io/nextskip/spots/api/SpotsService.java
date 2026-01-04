package io.nextskip.spots.api;

import io.nextskip.spots.model.BandActivity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public API for spot status, statistics, and band activity.
 *
 * <p>This interface provides access to:
 * <ul>
 *   <li>MQTT connection status and processing statistics</li>
 *   <li>Aggregated band activity data</li>
 *   <li>Recent spot queries</li>
 * </ul>
 *
 * <p>Implementation is provided by
 * {@link io.nextskip.spots.internal.SpotsServiceImpl}.
 */
public interface SpotsService {

    // ========================================================================
    // Phase 1: Status and Statistics
    // ========================================================================

    /**
     * Returns whether the MQTT source is currently connected.
     *
     * @return true if connected and receiving data
     */
    boolean isConnected();

    /**
     * Returns the name of the spot source.
     *
     * @return source name (e.g., "PSKReporter MQTT")
     */
    String getSourceName();

    /**
     * Returns the total count of spots in the database.
     *
     * @return spot count
     */
    long getSpotCount();

    /**
     * Returns the timestamp of the most recent spot.
     *
     * @return most recent spot time, or empty if no spots exist
     */
    Optional<Instant> getLastSpotTime();

    /**
     * Returns the total number of spots processed through the pipeline.
     *
     * <p>This count includes spots that were parsed and enriched,
     * regardless of whether they were successfully persisted.
     *
     * @return spots processed count
     */
    long getSpotsProcessed();

    /**
     * Returns the total number of batches persisted to the database.
     *
     * @return batches persisted count
     */
    long getBatchesPersisted();

    /**
     * Returns the count of spots received in the last specified minutes.
     *
     * @param minutes the time window in minutes
     * @return count of spots received within the time window
     */
    long getSpotCountSince(int minutes);

    // ========================================================================
    // Phase 2: Band Activity Aggregation
    // ========================================================================

    /**
     * Returns current band activity for all active bands.
     *
     * <p>Results are cached and refreshed periodically (typically every minute).
     * Only bands with recent activity (within the last 2 hours) are included.
     *
     * @return map of band name to aggregated activity data
     */
    Map<String, BandActivity> getCurrentActivity();

    /**
     * Returns activity for a specific band.
     *
     * @param band the band name (e.g., "20m")
     * @return band activity, or empty if no recent activity
     */
    Optional<BandActivity> getBandActivity(String band);

    /**
     * Returns the full response DTO for the frontend.
     *
     * <p>Includes all band activities, timestamp, and connection status.
     *
     * @return BandActivityResponse with all activity data
     */
    BandActivityResponse getBandActivityResponse();

    /**
     * Returns recent spots for a band within a time window.
     *
     * @param band   the band name (e.g., "20m")
     * @param window the time window to query
     * @return list of recent spots, most recent first
     */
    List<io.nextskip.spots.model.Spot> getRecentSpots(String band, Duration window);
}
