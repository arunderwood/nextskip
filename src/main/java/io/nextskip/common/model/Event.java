package io.nextskip.common.model;

import io.nextskip.common.api.Scoreable;

import java.time.Duration;
import java.time.Instant;

/**
 * Common interface for all time-bound events in the NextSkip system.
 *
 * <p>This interface provides a unified abstraction for scheduled activities such as contests,
 * meteor showers, field days, and other time-sensitive ham radio events. It extends
 * {@link Scoreable} to enable dashboard prioritization while adding time-based scheduling
 * information.
 *
 * <p>The Event abstraction follows the Open-Closed Principle: new event types can be added
 * by implementing this interface without modifying existing code. All events are interchangeable
 * where Event is expected (Liskov Substitution Principle).
 *
 * <p><strong>Examples of event types:</strong>
 * <ul>
 *     <li>Contest - scheduled amateur radio competitions</li>
 *     <li>MeteorShower - meteor scatter propagation opportunities</li>
 *     <li>FieldDay - special operating events</li>
 * </ul>
 */
public interface Event extends Scoreable {

    /**
     * Returns the human-readable name of this event.
     *
     * @return the event name (e.g., "ARRL 10-Meter Contest", "Geminids Meteor Shower")
     */
    String getName();

    /**
     * Returns the start time of this event in UTC.
     *
     * @return the instant when this event begins
     */
    Instant getStartTime();

    /**
     * Returns the end time of this event in UTC.
     *
     * @return the instant when this event ends
     */
    Instant getEndTime();

    /**
     * Returns the current status of this event.
     *
     * @return {@link EventStatus#UPCOMING} if event hasn't started,
     *         {@link EventStatus#ACTIVE} if currently ongoing,
     *         {@link EventStatus#ENDED} if finished
     */
    EventStatus getStatus();

    /**
     * Calculates the time remaining until the event ends (if active) or starts (if upcoming).
     *
     * <p>For active events, this represents how much time is left.
     * For upcoming events, this represents the countdown to start.
     * For ended events, this may return a negative duration or Duration.ZERO.
     *
     * @return the duration until the event ends (active) or starts (upcoming)
     */
    Duration getTimeRemaining();

    /**
     * Determines if this active event is ending soon.
     *
     * <p>"Ending soon" is typically defined as having less than 1 hour remaining,
     * though implementations may use different thresholds based on event duration.
     * This allows the dashboard to highlight time-sensitive opportunities with special alerts.
     *
     * @return true if the event is active and will end soon, false otherwise
     */
    boolean isEndingSoon();
}
