package io.nextskip.spots.api;

import io.nextskip.spots.model.BandActivity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Event published when band activity aggregation completes.
 *
 * <p>Other modules can listen for this event to react to activity changes.
 * For example, the propagation module could correlate band conditions with
 * actual observed activity, or a notification service could alert users
 * about band openings.
 *
 * <p>This event is published by {@link io.nextskip.spots.internal.scheduler.BandActivityRefreshService}
 * after each aggregation cycle (typically every minute).
 *
 * <p>Example listener:
 * <pre>{@code
 * @EventListener
 * public void onBandActivityChanged(BandActivityChangedEvent event) {
 *     Set<String> hotBands = event.getHotBands();
 *     if (!hotBands.isEmpty()) {
 *         log.info("Hot bands detected: {}", hotBands);
 *     }
 * }
 * }</pre>
 *
 * @param bandActivities map of band name to aggregated activity data
 */
public record BandActivityChangedEvent(
        Map<String, BandActivity> bandActivities
) {

    /**
     * Compact constructor with defensive copying.
     */
    public BandActivityChangedEvent {
        bandActivities = bandActivities != null
                ? Map.copyOf(bandActivities)
                : Map.of();
    }

    /**
     * Returns bands that are currently "hot" (favorable conditions).
     *
     * <p>A band is hot when {@link BandActivity#isFavorable()} returns true,
     * indicating high activity with positive trend and active DX paths.
     *
     * @return set of band names with favorable conditions
     */
    public Set<String> getHotBands() {
        return bandActivities.entrySet().stream()
                .filter(e -> e.getValue().isFavorable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the total spot count across all bands.
     *
     * @return sum of spot counts from all band activities
     */
    public int getTotalSpotCount() {
        return bandActivities.values().stream()
                .mapToInt(BandActivity::spotCount)
                .sum();
    }

    /**
     * Returns the number of bands with activity.
     *
     * @return count of bands in this event
     */
    public int getBandCount() {
        return bandActivities.size();
    }

    /**
     * Checks if there is any activity data.
     *
     * @return true if at least one band has activity data
     */
    public boolean hasActivity() {
        return !bandActivities.isEmpty();
    }

    /**
     * Gets activity for a specific band.
     *
     * @param band the band name (e.g., "20m")
     * @return the band activity, or null if not present
     */
    public BandActivity getActivity(String band) {
        return bandActivities.get(band);
    }
}
