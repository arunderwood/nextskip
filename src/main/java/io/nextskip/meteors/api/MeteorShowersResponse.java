package io.nextskip.meteors.api;

import io.nextskip.meteors.model.MeteorShower;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for meteor shower data exposed to frontend.
 *
 * @param showers List of active and upcoming meteor showers
 * @param activeCount Number of currently active showers
 * @param upcomingCount Number of upcoming showers
 * @param primaryShower The most significant shower (highest score), or null if none
 * @param lastUpdated Timestamp when this data was generated
 */
public record MeteorShowersResponse(
        List<MeteorShower> showers,
        int activeCount,
        int upcomingCount,
        MeteorShower primaryShower,
        Instant lastUpdated
) {
}
