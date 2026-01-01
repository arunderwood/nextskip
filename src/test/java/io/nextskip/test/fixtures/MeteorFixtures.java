package io.nextskip.test.fixtures;

import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.test.TestConstants;
import java.time.Duration;
import java.time.Instant;

/**
 * Test fixtures for meteor shower domain objects.
 *
 * <p>Provides factory methods and builders for creating test instances of
 * {@link MeteorShower} with various timing states (active at peak, upcoming, ended).
 */
public final class MeteorFixtures {

    private MeteorFixtures() {
        // Prevent instantiation
    }

    // ==========================================================================
    // Factory Methods for Common Scenarios
    // ==========================================================================

    /**
     * Creates a meteor shower currently at peak activity.
     *
     * <p>Peak started 6 hours ago and ends in 18 hours.
     * Visibility window extends 5 days before and 3 days after.
     *
     * @return a MeteorShower at peak activity
     */
    public static MeteorShower activeAtPeak() {
        Instant now = Instant.now();
        return meteorShower()
                .peakStart(now.minus(Duration.ofHours(6)))
                .peakEnd(now.plus(Duration.ofHours(18)))
                .visibilityStart(now.minus(Duration.ofDays(5)))
                .visibilityEnd(now.plus(Duration.ofDays(3)))
                .build();
    }

    /**
     * Creates a default meteor shower (alias for activeAtPeak).
     *
     * @return a MeteorShower at peak activity
     */
    public static MeteorShower defaultMeteorShower() {
        return activeAtPeak();
    }

    /**
     * Creates a meteor shower that is active but not at peak.
     *
     * <p>Within visibility window but peak is upcoming.
     *
     * @return a MeteorShower active but not at peak
     */
    public static MeteorShower activeNotAtPeak() {
        Instant now = Instant.now();
        return meteorShower()
                .peakStart(now.plus(Duration.ofHours(12)))
                .peakEnd(now.plus(Duration.ofHours(36)))
                .visibilityStart(now.minus(Duration.ofDays(2)))
                .visibilityEnd(now.plus(Duration.ofDays(5)))
                .build();
    }

    /**
     * Creates an upcoming meteor shower.
     *
     * <p>Visibility starts in 1 day, peak in 3 days.
     *
     * @return an upcoming MeteorShower
     */
    public static MeteorShower upcomingPeak() {
        Instant now = Instant.now();
        return meteorShower()
                .peakStart(now.plus(Duration.ofDays(3)))
                .peakEnd(now.plus(Duration.ofDays(4)))
                .visibilityStart(now.plus(Duration.ofDays(1)))
                .visibilityEnd(now.plus(Duration.ofDays(7)))
                .build();
    }

    /**
     * Creates an upcoming meteor shower starting in the specified hours.
     *
     * @param hoursUntilVisibility hours until visibility begins
     * @return an upcoming MeteorShower
     */
    public static MeteorShower upcomingPeak(int hoursUntilVisibility) {
        Instant now = Instant.now();
        return meteorShower()
                .visibilityStart(now.plus(Duration.ofHours(hoursUntilVisibility)))
                .peakStart(now.plus(Duration.ofHours(hoursUntilVisibility + 48)))
                .peakEnd(now.plus(Duration.ofHours(hoursUntilVisibility + 72)))
                .visibilityEnd(now.plus(Duration.ofHours(hoursUntilVisibility + 168)))
                .build();
    }

    /**
     * Creates a meteor shower past its peak.
     *
     * <p>Still within visibility window but peak has ended.
     *
     * @return a MeteorShower past peak
     */
    public static MeteorShower pastPeak() {
        Instant now = Instant.now();
        return meteorShower()
                .peakStart(now.minus(Duration.ofDays(2)))
                .peakEnd(now.minus(Duration.ofDays(1)))
                .visibilityStart(now.minus(Duration.ofDays(5)))
                .visibilityEnd(now.plus(Duration.ofDays(2)))
                .build();
    }

    /**
     * Creates an ended meteor shower.
     *
     * <p>Both peak and visibility window have passed.
     *
     * @return an ended MeteorShower
     */
    public static MeteorShower endedShower() {
        Instant now = Instant.now();
        return meteorShower()
                .peakStart(now.minus(Duration.ofDays(5)))
                .peakEnd(now.minus(Duration.ofDays(4)))
                .visibilityStart(now.minus(Duration.ofDays(8)))
                .visibilityEnd(now.minus(Duration.ofDays(1)))
                .build();
    }

    /**
     * Creates an imminent meteor shower (visibility starting very soon).
     *
     * <p>Visibility starts in 6 hours.
     *
     * @return an imminent MeteorShower
     */
    public static MeteorShower imminentShower() {
        Instant now = Instant.now();
        return meteorShower()
                .visibilityStart(now.plus(Duration.ofHours(6)))
                .peakStart(now.plus(Duration.ofDays(2)))
                .peakEnd(now.plus(Duration.ofDays(3)))
                .visibilityEnd(now.plus(Duration.ofDays(7)))
                .build();
    }

    /**
     * Creates a MeteorShower builder for customization.
     *
     * @return a new MeteorShowerBuilder with default values
     */
    public static MeteorShowerBuilder meteorShower() {
        return new MeteorShowerBuilder();
    }

    // ==========================================================================
    // MeteorShower Builder
    // ==========================================================================

    /**
     * Builder for creating customized MeteorShower instances.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // Fluent builder pattern
    public static class MeteorShowerBuilder {
        private String name = TestConstants.DEFAULT_SHOWER_NAME;
        private String code = TestConstants.DEFAULT_SHOWER_CODE;
        private Instant peakStart = Instant.now();
        private Instant peakEnd = Instant.now().plus(Duration.ofHours(24));
        private Instant visibilityStart = Instant.now().minus(Duration.ofDays(5));
        private Instant visibilityEnd = Instant.now().plus(Duration.ofDays(5));
        private int peakZhr = TestConstants.DEFAULT_PEAK_ZHR;
        private String parentBody = TestConstants.DEFAULT_PARENT_BODY;
        private String infoUrl = TestConstants.DEFAULT_INFO_URL;

        public MeteorShowerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MeteorShowerBuilder code(String code) {
            this.code = code;
            return this;
        }

        public MeteorShowerBuilder peakStart(Instant peakStart) {
            this.peakStart = peakStart;
            return this;
        }

        public MeteorShowerBuilder peakEnd(Instant peakEnd) {
            this.peakEnd = peakEnd;
            return this;
        }

        public MeteorShowerBuilder visibilityStart(Instant visibilityStart) {
            this.visibilityStart = visibilityStart;
            return this;
        }

        public MeteorShowerBuilder visibilityEnd(Instant visibilityEnd) {
            this.visibilityEnd = visibilityEnd;
            return this;
        }

        public MeteorShowerBuilder peakZhr(int peakZhr) {
            this.peakZhr = peakZhr;
            return this;
        }

        public MeteorShowerBuilder parentBody(String parentBody) {
            this.parentBody = parentBody;
            return this;
        }

        public MeteorShowerBuilder infoUrl(String infoUrl) {
            this.infoUrl = infoUrl;
            return this;
        }

        /**
         * Convenience method to set a 24-hour peak window starting at the specified time.
         *
         * @param start the peak start time
         * @return this builder
         */
        public MeteorShowerBuilder peakAt(Instant start) {
            this.peakStart = start;
            this.peakEnd = start.plus(Duration.ofHours(24));
            return this;
        }

        /**
         * Convenience method to set a peak window with a specific duration.
         *
         * @param start the peak start time
         * @param duration how long peak lasts
         * @return this builder
         */
        public MeteorShowerBuilder peakAt(Instant start, Duration duration) {
            this.peakStart = start;
            this.peakEnd = start.plus(duration);
            return this;
        }

        /**
         * Convenience method to set visibility window.
         *
         * @param start when visibility begins
         * @param end when visibility ends
         * @return this builder
         */
        public MeteorShowerBuilder visibleBetween(Instant start, Instant end) {
            this.visibilityStart = start;
            this.visibilityEnd = end;
            return this;
        }

        public MeteorShower build() {
            return new MeteorShower(
                    name,
                    code,
                    peakStart,
                    peakEnd,
                    visibilityStart,
                    visibilityEnd,
                    peakZhr,
                    parentBody,
                    infoUrl
            );
        }
    }
}
