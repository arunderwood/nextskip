package io.nextskip.activations.model;

import io.nextskip.common.api.Scoreable;
import java.time.Instant;
import java.time.Duration;

/**
 * Represents an individual amateur radio activation event (POTA or SOTA).
 *
 * <p>Implements scoring based on recency - activations with recent spot times
 * score higher as they're more likely to be currently active.</p>
 *
 * <p>Follows SOLID principles:
 * <ul>
 *   <li><b>SRP</b>: {@code spottedAt} = when someone last reported hearing the station (scoring),
 *       {@code lastSeenAt} = when our API client observed the spot (cache management)</li>
 *   <li><b>DIP</b>: Scoring methods accept {@code Instant asOf} parameter
 *       for deterministic testing (no dependency on {@code Instant.now()})</li>
 * </ul>
 * Location-specific information is encapsulated in {@link ActivationLocation}
 * implementations ({@link Park} or {@link Summit}).</p>
 *
 * @param spotId Unique identifier for the spot
 * @param activatorCallsign Callsign of the activator
 * @param type Type of activation (POTA or SOTA)
 * @param frequency Operating frequency in kHz
 * @param mode Operating mode (e.g., "SSB", "CW", "FT8")
 * @param spottedAt Timestamp when someone last reported hearing the station (used for scoring)
 * @param lastSeenAt Timestamp when the activation was last observed in an API refresh (cache only)
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
        Instant lastSeenAt,
        Integer qsoCount,
        String source,
        ActivationLocation location
) implements Scoreable {

    // Recency scoring thresholds (in minutes)
    private static final long VERY_FRESH_THRESHOLD_MINUTES = 5;
    private static final long FRESH_THRESHOLD_MINUTES = 15;
    private static final long AGING_THRESHOLD_MINUTES = 30;
    private static final long STALE_THRESHOLD_MINUTES = 60;

    /**
     * An activation is favorable if it was spotted within the past 15 minutes.
     *
     * <p>Uses the current time as reference. For deterministic testing, use
     * {@link #isFavorable(Instant)} with an explicit reference time.</p>
     *
     * @return true if the activation was recently spotted and likely still active
     */
    @Override
    public boolean isFavorable() {
        return isFavorable(Instant.now());
    }

    /**
     * An activation is favorable if it was spotted within the past 15 minutes.
     *
     * <p>This method follows the Dependency Inversion Principle by accepting an
     * explicit reference time, enabling deterministic unit tests.</p>
     *
     * @param asOf the reference time to calculate recency from
     * @return true if the activation was recently spotted and likely still active
     */
    public boolean isFavorable(Instant asOf) {
        if (spottedAt == null) {
            return false;
        }
        Duration age = Duration.between(spottedAt, asOf);
        return age.toMinutes() <= FRESH_THRESHOLD_MINUTES;
    }

    /**
     * Calculate score based on how recently the activation was spotted.
     *
     * <p>Uses the current time as reference. For deterministic testing, use
     * {@link #getScore(Instant)} with an explicit reference time.</p>
     *
     * @return score from 0-100 based on recency
     */
    @Override
    public int getScore() {
        return getScore(Instant.now());
    }

    /**
     * Calculate score based on how recently the activation was spotted.
     *
     * <p>This method follows the Dependency Inversion Principle by accepting an
     * explicit reference time, enabling deterministic unit tests.</p>
     *
     * <p>Scoring algorithm (based on time since spotted):
     * <ul>
     *   <li>0-5 minutes: 100 points (very fresh)</li>
     *   <li>5-15 minutes: 80-100 points (fresh, linear decay)</li>
     *   <li>15-30 minutes: 20-80 points (aging, linear decay)</li>
     *   <li>30-60 minutes: 0-20 points (stale, linear decay)</li>
     *   <li>60+ minutes: 0 points (expired)</li>
     * </ul>
     *
     * <p>Scoring is based solely on {@code spottedAt}. If {@code spottedAt}
     * is null (which should not happen in production), returns 0.</p>
     *
     * @param asOf the reference time to calculate recency from
     * @return score from 0-100 based on recency
     */
    public int getScore(Instant asOf) {
        if (spottedAt == null) {
            return 0;
        }

        Duration age = Duration.between(spottedAt, asOf);
        long minutes = age.toMinutes();

        if (minutes < 0) {
            // Future timestamp (shouldn't happen, but handle gracefully)
            return 100;
        } else if (minutes <= VERY_FRESH_THRESHOLD_MINUTES) {
            // Very fresh: 100 points
            return 100;
        } else if (minutes <= FRESH_THRESHOLD_MINUTES) {
            // Fresh: 80-100 points (linear decay)
            return (int) (100 - ((minutes - VERY_FRESH_THRESHOLD_MINUTES) * 2));
        } else if (minutes <= AGING_THRESHOLD_MINUTES) {
            // Aging: 20-80 points (linear decay)
            return (int) (80 - ((minutes - FRESH_THRESHOLD_MINUTES) * 4));
        } else if (minutes <= STALE_THRESHOLD_MINUTES) {
            // Stale: 0-20 points (linear decay)
            return (int) Math.max(0, 20 - ((minutes - AGING_THRESHOLD_MINUTES) * 0.67));
        } else {
            // Very stale: 0 points
            return 0;
        }
    }
}
