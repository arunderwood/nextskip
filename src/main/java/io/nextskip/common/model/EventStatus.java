package io.nextskip.common.model;

/**
 * Represents the current lifecycle status of a time-bound event.
 *
 * <p>This enum is used by {@link Event} implementations to indicate whether
 * an event is scheduled for the future, currently happening, or has concluded.
 */
public enum EventStatus {
    /**
     * Event has not yet started. It is scheduled for a future time.
     */
    UPCOMING,

    /**
     * Event is currently ongoing. Start time has passed and end time has not yet occurred.
     */
    ACTIVE,

    /**
     * Event has concluded. End time has passed.
     */
    ENDED
}
