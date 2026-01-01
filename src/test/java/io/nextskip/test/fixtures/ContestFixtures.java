package io.nextskip.test.fixtures;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.model.Contest;
import io.nextskip.test.TestConstants;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Test fixtures for contest-related domain objects.
 *
 * <p>Provides factory methods and builders for creating test instances of
 * {@link Contest} with various timing states (active, upcoming, ended).
 */
public final class ContestFixtures {

    private ContestFixtures() {
        // Prevent instantiation
    }

    // ==========================================================================
    // Factory Methods for Common Scenarios
    // ==========================================================================

    /**
     * Creates an active contest (currently in progress).
     *
     * <p>Contest started 1 hour ago and ends in 23 hours.
     *
     * @return an active Contest
     */
    public static Contest activeContest() {
        Instant now = Instant.now();
        return contest()
                .startTime(now.minus(Duration.ofHours(1)))
                .endTime(now.plus(Duration.ofHours(23)))
                .build();
    }

    /**
     * Creates a default contest (alias for activeContest).
     *
     * @return an active Contest
     */
    public static Contest defaultContest() {
        return activeContest();
    }

    /**
     * Creates an upcoming contest starting in 1 hour.
     *
     * @return an upcoming Contest
     */
    public static Contest upcomingContest() {
        Instant now = Instant.now();
        return contest()
                .startTime(now.plus(Duration.ofHours(1)))
                .endTime(now.plus(Duration.ofHours(25)))
                .build();
    }

    /**
     * Creates an upcoming contest starting in the specified hours.
     *
     * @param hoursUntilStart hours until contest starts
     * @return an upcoming Contest
     */
    public static Contest upcomingContest(int hoursUntilStart) {
        Instant now = Instant.now();
        return contest()
                .startTime(now.plus(Duration.ofHours(hoursUntilStart)))
                .endTime(now.plus(Duration.ofHours(hoursUntilStart + 24)))
                .build();
    }

    /**
     * Creates a contest that ended 1 hour ago.
     *
     * @return an ended Contest
     */
    public static Contest endedContest() {
        Instant now = Instant.now();
        return contest()
                .startTime(now.minus(Duration.ofHours(25)))
                .endTime(now.minus(Duration.ofHours(1)))
                .build();
    }

    /**
     * Creates a contest that ended the specified hours ago.
     *
     * @param hoursAgo hours since contest ended
     * @return an ended Contest
     */
    public static Contest endedContest(int hoursAgo) {
        Instant now = Instant.now();
        return contest()
                .startTime(now.minus(Duration.ofHours(hoursAgo + 24)))
                .endTime(now.minus(Duration.ofHours(hoursAgo)))
                .build();
    }

    /**
     * Creates an imminent contest (starting very soon, within 6 hours).
     *
     * @return a Contest starting in 2 hours
     */
    public static Contest imminentContest() {
        Instant now = Instant.now();
        return contest()
                .startTime(now.plus(Duration.ofHours(2)))
                .endTime(now.plus(Duration.ofHours(26)))
                .build();
    }

    /**
     * Creates a contest ending soon (within 1 hour).
     *
     * @return an active Contest ending soon
     */
    public static Contest endingSoonContest() {
        Instant now = Instant.now();
        return contest()
                .startTime(now.minus(Duration.ofHours(23)))
                .endTime(now.plus(Duration.ofMinutes(30)))
                .build();
    }

    /**
     * Creates a Contest builder for customization.
     *
     * @return a new ContestBuilder with default values
     */
    public static ContestBuilder contest() {
        return new ContestBuilder();
    }

    // ==========================================================================
    // Contest Builder
    // ==========================================================================

    /**
     * Builder for creating customized Contest instances.
     */
    public static class ContestBuilder {
        private String name = TestConstants.DEFAULT_CONTEST_NAME;
        private Instant startTime = Instant.now();
        private Instant endTime = Instant.now().plus(Duration.ofHours(24));
        private Set<FrequencyBand> bands = Set.of(
                FrequencyBand.BAND_160M,
                FrequencyBand.BAND_80M,
                FrequencyBand.BAND_40M,
                FrequencyBand.BAND_20M,
                FrequencyBand.BAND_15M,
                FrequencyBand.BAND_10M
        );
        private Set<String> modes = Set.of("CW", "SSB");
        private String sponsor = TestConstants.DEFAULT_CONTEST_SPONSOR;
        private String calendarSourceUrl = "https://contestcalendar.com/test";
        private String officialRulesUrl = "https://arrl.org/test-contest-rules";

        public ContestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ContestBuilder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public ContestBuilder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public ContestBuilder bands(Set<FrequencyBand> bands) {
            this.bands = bands;
            return this;
        }

        public ContestBuilder modes(Set<String> modes) {
            this.modes = modes;
            return this;
        }

        public ContestBuilder sponsor(String sponsor) {
            this.sponsor = sponsor;
            return this;
        }

        public ContestBuilder calendarSourceUrl(String calendarSourceUrl) {
            this.calendarSourceUrl = calendarSourceUrl;
            return this;
        }

        public ContestBuilder officialRulesUrl(String officialRulesUrl) {
            this.officialRulesUrl = officialRulesUrl;
            return this;
        }

        /**
         * Convenience method to set a 24-hour duration starting at the specified time.
         *
         * @param start the start time
         * @return this builder
         */
        public ContestBuilder starting(Instant start) {
            this.startTime = start;
            this.endTime = start.plus(Duration.ofHours(24));
            return this;
        }

        /**
         * Convenience method to set a duration starting at the specified time.
         *
         * @param start the start time
         * @param duration how long the contest lasts
         * @return this builder
         */
        public ContestBuilder starting(Instant start, Duration duration) {
            this.startTime = start;
            this.endTime = start.plus(duration);
            return this;
        }

        public Contest build() {
            return new Contest(
                    name,
                    startTime,
                    endTime,
                    bands,
                    modes,
                    sponsor,
                    calendarSourceUrl,
                    officialRulesUrl
            );
        }
    }
}
