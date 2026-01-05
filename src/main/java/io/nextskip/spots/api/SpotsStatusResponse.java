package io.nextskip.spots.api;

import java.time.Instant;

/**
 * Response DTO for spot system status information.
 *
 * <p>Provides operational status for the spots ingestion pipeline:
 * MQTT connection state, spot counts, and processing statistics.
 *
 * @param connected       whether the MQTT source is currently connected
 * @param sourceName      name of the spot source (e.g., "PSKReporter MQTT")
 * @param totalSpots      total spots in the database
 * @param lastSpotTime    timestamp of the most recent spot, null if none
 * @param spotsProcessed  total spots processed through the pipeline
 */
public record SpotsStatusResponse(
        boolean connected,
        String sourceName,
        long totalSpots,
        Instant lastSpotTime,
        long spotsProcessed
) {

    /**
     * Returns whether the system is healthy.
     *
     * <p>Healthy means connected and has processed at least one spot.
     *
     * @return true if system is healthy
     */
    public boolean isHealthy() {
        return connected && spotsProcessed > 0;
    }

    /**
     * Returns a human-readable status string.
     *
     * @return status description
     */
    public String getStatusDescription() {
        if (!connected) {
            return "Disconnected from " + sourceName;
        }
        if (spotsProcessed == 0) {
            return "Connected, waiting for first spot";
        }
        return "Connected, " + totalSpots + " spots in database";
    }
}
