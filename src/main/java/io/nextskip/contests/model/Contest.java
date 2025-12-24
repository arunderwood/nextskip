package io.nextskip.contests.model;

import io.nextskip.common.model.Event;
import io.nextskip.common.model.EventStatus;
import io.nextskip.common.model.FrequencyBand;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Represents an amateur radio contest or competition event.
 *
 * <p>Implements the {@link Event} interface to provide consistent time-based scheduling
 * and scoring across the NextSkip dashboard. Contests are scored based on their current
 * status (active contests score highest) and time remaining.
 *
 * <p>This record encapsulates both the core event timing and contest-specific metadata
 * such as bands, modes, sponsor organization, and reference URLs.
 *
 * @param name Contest name (e.g., "ARRL 10-Meter Contest", "CQ WW DX Contest")
 * @param startTime When the contest begins (UTC)
 * @param endTime When the contest ends (UTC)
 * @param bands Set of frequency bands permitted in this contest (160m, 80m, 40m, etc.)
 * @param modes Set of modes permitted (CW, SSB, FT8, etc.)
 * @param sponsor Sponsoring organization (ARRL, CQ, etc.)
 * @param calendarSourceUrl URL to contest details on calendar source (contestcalendar.com)
 * @param officialRulesUrl URL to official contest rules from sponsor
 */
public record Contest(
        String name,
        Instant startTime,
        Instant endTime,
        Set<FrequencyBand> bands,
        Set<String> modes,
        String sponsor,
        String calendarSourceUrl,
        String officialRulesUrl
) implements Event {

    /**
     * Compact constructor for defensive copying of mutable collections.
     */
    public Contest {
        bands = bands != null ? Set.copyOf(bands) : Set.of();
        modes = modes != null ? Set.copyOf(modes) : Set.of();
    }

    /**
     * Returns the event name.
     *
     * @return the contest name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the contest start time.
     *
     * @return when the contest begins (UTC)
     */
    @Override
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns the contest end time.
     *
     * @return when the contest ends (UTC)
     */
    @Override
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Determines the current status of this contest relative to the current time.
     *
     * @return UPCOMING if contest hasn't started, ACTIVE if ongoing, ENDED if finished
     */
    @Override
    public EventStatus getStatus() {
        Instant now = Instant.now();
        if (now.isBefore(startTime)) {
            return EventStatus.UPCOMING;
        } else if (now.isAfter(endTime)) {
            return EventStatus.ENDED;
        } else {
            return EventStatus.ACTIVE;
        }
    }

    /**
     * Calculates time remaining until the contest ends (if active) or starts (if upcoming).
     *
     * @return duration until end (if active) or start (if upcoming), negative if ended
     */
    @Override
    public Duration getTimeRemaining() {
        Instant now = Instant.now();
        EventStatus status = getStatus();

        return switch (status) {
            case UPCOMING -> Duration.between(now, startTime);
            case ACTIVE -> Duration.between(now, endTime);
            case ENDED -> Duration.between(endTime, now).negated();
        };
    }

    /**
     * Returns time remaining as total seconds for frontend serialization.
     * Hilla doesn't serialize Duration properly, so we expose seconds as a plain long.
     *
     * @return seconds until end (if active) or start (if upcoming), negative if ended
     */
    public long getTimeRemainingSeconds() {
        return getTimeRemaining().getSeconds();
    }

    /**
     * Determines if this active contest is ending soon (within 1 hour).
     *
     * @return true if contest is active and will end within 1 hour
     */
    @Override
    public boolean isEndingSoon() {
        if (getStatus() != EventStatus.ACTIVE) {
            return false;
        }
        return getTimeRemaining().toHours() < 1;
    }

    /**
     * A contest is favorable if it is currently active or starts within the next 6 hours.
     *
     * @return true if the contest is active or starting soon
     */
    @Override
    public boolean isFavorable() {
        EventStatus status = getStatus();
        if (status == EventStatus.ACTIVE) {
            return true;
        }
        if (status == EventStatus.UPCOMING) {
            Duration timeToStart = getTimeRemaining();
            return timeToStart.toHours() <= 6;
        }
        return false;
    }

    /**
     * Calculate score based on contest status and timing.
     *
     * <p>Scoring algorithm:
     * <ul>
     *   <li>ACTIVE: 100 points (contest happening now)</li>
     *   <li>UPCOMING (0-6 hours): 80-100 points (linear decay)</li>
     *   <li>UPCOMING (6-24 hours): 40-80 points (linear decay)</li>
     *   <li>UPCOMING (24-72 hours): 20-40 points (linear decay)</li>
     *   <li>UPCOMING (72+ hours): 10 points (far future)</li>
     *   <li>ENDED: 0 points</li>
     * </ul>
     *
     * @return score from 0-100 based on status and timing
     */
    @Override
    public int getScore() {
        EventStatus status = getStatus();

        return switch (status) {
            case ACTIVE -> 100;
            case UPCOMING -> {
                Duration timeToStart = getTimeRemaining();
                long hours = timeToStart.toHours();

                if (hours <= 6) {
                    // 0-6 hours: 80-100 points (linear decay)
                    yield (int) (100 - (hours * 3.33));
                } else if (hours <= 24) {
                    // 6-24 hours: 40-80 points (linear decay)
                    yield (int) (80 - ((hours - 6) * 2.22));
                } else if (hours <= 72) {
                    // 24-72 hours: 20-40 points (linear decay)
                    yield (int) (40 - ((hours - 24) * 0.42));
                } else {
                    // 72+ hours: 10 points (far future)
                    yield 10;
                }
            }
            case ENDED -> 0;
        };
    }
}
