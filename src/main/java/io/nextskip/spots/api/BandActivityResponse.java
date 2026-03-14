package io.nextskip.spots.api;

import io.nextskip.spots.model.BandActivity;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for band activity data sent to the frontend.
 *
 * <p>This is the primary response from {@link SpotsEndpoint#getBandActivity()}.
 * Contains all band+mode activities, a timestamp, and MQTT connection status.
 *
 * <p>Map keys use composite format {@code "{band}_{mode}"} (e.g., "20m_FT8",
 * "20m_FT4"). Each band can have multiple entries — one per active mode.
 *
 * @param bandActivities map of composite key to aggregated activity data
 * @param timestamp      when this response was generated
 * @param mqttConnected  whether the MQTT source is currently connected
 */
public record BandActivityResponse(
        Map<String, BandActivity> bandActivities,
        Instant timestamp,
        boolean mqttConnected
) {

    /**
     * Compact constructor with defensive copying for the map.
     */
    public BandActivityResponse {
        bandActivities = bandActivities != null
                ? Map.copyOf(bandActivities)
                : Map.of();
    }

    /**
     * Returns the number of bands with activity.
     *
     * @return count of bands
     */
    public int getBandCount() {
        return bandActivities.size();
    }

    /**
     * Returns the total spot count across all bands.
     *
     * @return sum of spot counts
     */
    public int getTotalSpotCount() {
        return bandActivities.values().stream()
                .mapToInt(BandActivity::spotCount)
                .sum();
    }

    /**
     * Checks if there is any activity data.
     *
     * @return true if at least one band has activity
     */
    public boolean hasActivity() {
        return !bandActivities.isEmpty();
    }
}
