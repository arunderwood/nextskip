package io.nextskip.meteors.model;

import io.nextskip.common.model.Event;
import io.nextskip.common.model.EventStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a meteor shower event for meteor scatter (MS) propagation.
 *
 * <p>Implements the {@link Event} interface to provide consistent time-based scheduling
 * and scoring across the NextSkip dashboard. Meteor showers are scored based on their
 * proximity to peak activity and the calculated ZHR.
 *
 * <p>The visibility window represents the broader active period during which MS contacts
 * are possible, while the peak window represents optimal activity.
 *
 * @param name Display name with year (e.g., "Perseids 2025")
 * @param code Unique shower code (e.g., "PER")
 * @param peakStart Start of peak activity window (UTC)
 * @param peakEnd End of peak activity window (UTC)
 * @param visibilityStart Start of broader active period (UTC)
 * @param visibilityEnd End of broader active period (UTC)
 * @param peakZhr Zenithal Hourly Rate at peak
 * @param parentBody Parent comet or asteroid
 * @param infoUrl URL for more information
 */
public record MeteorShower(
        String name,
        String code,
        Instant peakStart,
        Instant peakEnd,
        Instant visibilityStart,
        Instant visibilityEnd,
        int peakZhr,
        String parentBody,
        String infoUrl
) implements Event {

    /**
     * Sigma value for Gaussian ZHR decay (in hours).
     * Typical meteor showers have a ~24 hour half-width.
     */
    private static final double SIGMA_HOURS = 24.0;

    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the visibility start time (when MS contacts become possible).
     * This is the "start time" for Event interface purposes.
     */
    @Override
    public Instant getStartTime() {
        return visibilityStart;
    }

    /**
     * Returns the visibility end time (when MS contacts are no longer viable).
     */
    @Override
    public Instant getEndTime() {
        return visibilityEnd;
    }

    /**
     * Determines the current status of this meteor shower.
     *
     * @return UPCOMING if before visibility window, ACTIVE if within window, ENDED if past
     */
    @Override
    public EventStatus getStatus() {
        Instant now = Instant.now();
        if (now.isBefore(visibilityStart)) {
            return EventStatus.UPCOMING;
        } else if (now.isAfter(visibilityEnd)) {
            return EventStatus.ENDED;
        } else {
            return EventStatus.ACTIVE;
        }
    }

    /**
     * Calculates time remaining until the shower ends (if active) or starts (if upcoming).
     */
    @Override
    public Duration getTimeRemaining() {
        Instant now = Instant.now();
        EventStatus status = getStatus();

        return switch (status) {
            case UPCOMING -> Duration.between(now, visibilityStart);
            case ACTIVE -> Duration.between(now, visibilityEnd);
            case ENDED -> Duration.between(visibilityEnd, now).negated();
        };
    }

    /**
     * Returns time remaining as seconds for frontend serialization.
     * Hilla doesn't serialize Duration properly, so we expose seconds as a plain long.
     */
    public long getTimeRemainingSeconds() {
        return getTimeRemaining().getSeconds();
    }

    /**
     * Returns true if the shower is active and peak is ending soon (within 6 hours).
     */
    @Override
    public boolean isEndingSoon() {
        if (getStatus() != EventStatus.ACTIVE) {
            return false;
        }
        // Check if peak is ending soon (within 6 hours)
        Instant now = Instant.now();
        return now.isAfter(peakEnd.minus(Duration.ofHours(6))) && now.isBefore(peakEnd);
    }

    /**
     * Determines if conditions are currently favorable for MS contacts.
     *
     * <p>Favorable when:
     * <ul>
     *   <li>Currently at peak (between peakStart and peakEnd), OR</li>
     *   <li>Peak starts within the next 12 hours</li>
     * </ul>
     */
    @Override
    public boolean isFavorable() {
        Instant now = Instant.now();
        EventStatus status = getStatus();

        if (status == EventStatus.ACTIVE) {
            // During peak is most favorable
            if (!now.isBefore(peakStart) && !now.isAfter(peakEnd)) {
                return true;
            }
            // Near peak (within 12 hours) is also favorable
            Duration timeToPeak = Duration.between(now, peakStart);
            return timeToPeak.toHours() >= 0 && timeToPeak.toHours() <= 12;
        }

        if (status == EventStatus.UPCOMING) {
            Duration timeToVisibility = Duration.between(now, visibilityStart);
            return timeToVisibility.toHours() <= 12;
        }

        return false;
    }

    /**
     * Calculate score based on proximity to peak and ZHR.
     *
     * <p>Scoring algorithm:
     * <ul>
     *   <li>During peak: 85-100 (higher ZHR = higher score)</li>
     *   <li>Active but not at peak: 40-84 (based on calculated current ZHR)</li>
     *   <li>Upcoming 0-24 hours: 60-80 (linear decay)</li>
     *   <li>Upcoming 24-72 hours: 30-60 (linear decay)</li>
     *   <li>Upcoming 72+ hours: 15</li>
     *   <li>Ended: 0</li>
     * </ul>
     */
    @Override
    public int getScore() {
        EventStatus status = getStatus();
        Instant now = Instant.now();

        return switch (status) {
            case ACTIVE -> {
                // During peak window - score 85-100 based on ZHR
                if (!now.isBefore(peakStart) && !now.isAfter(peakEnd)) {
                    // Higher ZHR showers score higher
                    // Map ZHR 10-150 to score 85-100
                    int zhrBonus = (int) Math.min(15, peakZhr / 10.0);
                    yield 85 + zhrBonus;
                }
                // Active but not at peak - use Gaussian decay
                int currentZhr = getCurrentZhr();
                // Map current ZHR to score 40-84
                double zhrRatio = (double) currentZhr / peakZhr;
                yield (int) (40 + (zhrRatio * 44));
            }
            case UPCOMING -> {
                Duration timeToStart = Duration.between(now, visibilityStart);
                long hours = timeToStart.toHours();

                if (hours <= 24) {
                    // 0-24 hours: 60-80 points (linear decay)
                    yield (int) (80 - (hours * 0.83));
                } else if (hours <= 72) {
                    // 24-72 hours: 30-60 points (linear decay)
                    yield (int) (60 - ((hours - 24) * 0.625));
                } else {
                    // 72+ hours: fixed 15 points
                    yield 15;
                }
            }
            case ENDED -> 0;
        };
    }

    /**
     * Calculate the current ZHR based on Gaussian decay from peak.
     *
     * <p>Uses a bell curve centered on the peak midpoint:
     * ZHR(t) = peakZhr * exp(-0.5 * ((t - peak) / sigma)^2)
     *
     * @return calculated current ZHR (minimum 1 if within visibility window)
     */
    public int getCurrentZhr() {
        Instant now = Instant.now();
        EventStatus status = getStatus();

        if (status == EventStatus.ENDED) {
            return 0;
        }

        if (status == EventStatus.UPCOMING) {
            // Before visibility - return a fraction based on how close
            return 1;
        }

        // Calculate peak midpoint
        Instant peakMidpoint = peakStart.plus(Duration.between(peakStart, peakEnd).dividedBy(2));

        // Hours from peak midpoint
        double hoursFromPeak = Math.abs(Duration.between(now, peakMidpoint).toHours());

        // Gaussian decay
        double decay = Math.exp(-0.5 * Math.pow(hoursFromPeak / SIGMA_HOURS, 2));
        int currentZhr = (int) Math.round(peakZhr * decay);

        // Ensure minimum of 1 while active
        return Math.max(1, currentZhr);
    }

    /**
     * Returns true if currently at peak activity.
     */
    public boolean isAtPeak() {
        Instant now = Instant.now();
        return !now.isBefore(peakStart) && !now.isAfter(peakEnd);
    }

    /**
     * Returns the duration until peak starts (or negative if past peak).
     */
    public Duration getTimeToPeak() {
        Instant now = Instant.now();
        if (now.isBefore(peakStart)) {
            return Duration.between(now, peakStart);
        } else if (now.isAfter(peakEnd)) {
            return Duration.between(peakEnd, now).negated();
        } else {
            // Currently at peak
            return Duration.ZERO;
        }
    }

    /**
     * Returns time to peak as seconds for frontend serialization.
     */
    public long getTimeToPeakSeconds() {
        return getTimeToPeak().getSeconds();
    }
}
