package io.nextskip.activations.model;

import io.nextskip.common.api.Scoreable;
import java.time.Instant;
import java.time.Duration;

/**
 * Represents an individual amateur radio activation event (POTA or SOTA).
 *
 * <p>Implements scoring based on recency - newer activations score higher
 * as they're more likely to be currently active.</p>
 *
 * <p>Follows SOLID principles by separating activation events from location data.
 * Location-specific information is encapsulated in {@link ActivationLocation}
 * implementations ({@link Park} or {@link Summit}).</p>
 *
 * @param spotId Unique identifier for the spot
 * @param activatorCallsign Callsign of the activator
 * @param type Type of activation (POTA or SOTA)
 * @param frequency Operating frequency in kHz
 * @param mode Operating mode (e.g., "SSB", "CW", "FT8")
 * @param spottedAt Timestamp when the activation was spotted
 * @param qsoCount Number of QSOs completed (if available)
 * @param source Data source identifier
 * @param location Location being activated (Park for POTA, Summit for SOTA)
 */
public record Activation(
        String spotId,
        String activatorCallsign,
        ActivationType type,
        Double frequency,
        String mode,
        Instant spottedAt,
        Integer qsoCount,
        String source,
        ActivationLocation location
) implements Scoreable {

    /**
     * An activation is favorable if it was spotted within the last 15 minutes.
     *
     * @return true if the activation is recent and likely still active
     */
    @Override
    public boolean isFavorable() {
        if (spottedAt == null) {
            return false;
        }
        Duration age = Duration.between(spottedAt, Instant.now());
        return age.toMinutes() <= 15;
    }

    /**
     * Calculate score based on recency.
     *
     * <p>Scoring algorithm:
     * <ul>
     *   <li>0-5 minutes old: 100 points (very fresh)</li>
     *   <li>5-15 minutes old: 80-100 points (fresh, linear decay)</li>
     *   <li>15-30 minutes old: 20-80 points (aging, linear decay)</li>
     *   <li>30+ minutes old: 0-20 points (stale, linear decay to 0 at 60 minutes)</li>
     * </ul>
     *
     * @return score from 0-100 based on recency
     */
    @Override
    public int getScore() {
        if (spottedAt == null) {
            return 0;
        }

        Duration age = Duration.between(spottedAt, Instant.now());
        long minutes = age.toMinutes();

        if (minutes < 0) {
            // Future timestamp (shouldn't happen, but handle gracefully)
            return 100;
        } else if (minutes <= 5) {
            // Very fresh: 100 points
            return 100;
        } else if (minutes <= 15) {
            // Fresh: 80-100 points (linear decay)
            return (int) (100 - ((minutes - 5) * 2));
        } else if (minutes <= 30) {
            // Aging: 20-80 points (linear decay)
            return (int) (80 - ((minutes - 15) * 4));
        } else if (minutes <= 60) {
            // Stale: 0-20 points (linear decay)
            return (int) Math.max(0, 20 - ((minutes - 30) * 0.67));
        } else {
            // Very stale: 0 points
            return 0;
        }
    }
}
