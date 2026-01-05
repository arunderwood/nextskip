package io.nextskip.spots.api;

import io.nextskip.spots.model.BandActivity;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for band activity data sent to the frontend.
 *
 * <p>This is the primary response from {@link SpotsEndpoint#getBandActivity()}.
 * Contains all band activities, a timestamp, and MQTT connection status.
 *
 * @param bandActivities map of band name to aggregated activity data
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
