package io.nextskip.contests.model;

import io.nextskip.common.model.EventStatus;
import io.nextskip.common.model.FrequencyBand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.nextskip.test.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Contest model.
 *
 * <p>Tests use invariant-based assertions to verify scoring contracts
 * without coupling to specific algorithm coefficients.
 */
class ContestTest {

    private static final String TEST_CONTEST = "Test Contest";
    private static final String TEST_SIMPLE = "Test";
    private static final String MODE_CW = "CW";
    private static final String ORGANIZER_ARRL = "ARRL";
    private static final String URL_EXAMPLE = "https://example.com";
    private static final String URL_EXAMPLE_RULES = "https://example.com/rules";
    private static final String UPCOMING_CONTEST = "Upcoming Contest";
    private static final String ACTIVE_CONTEST = "Active Contest";
    private static final String ENDED_CONTEST = "Ended Contest";

    // ==========================================================================
    // Status Tests
    // ==========================================================================

    @Nested
    class StatusTests {

        @Test
        void testGetStatus_Active() {
            Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

            Contest contest = createContest(ACTIVE_CONTEST, start, end);

            assertThat(contest.getStatus()).isEqualTo(EventStatus.ACTIVE);
        }

        @Test
        void testGetStatus_Upcoming() {
            Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

            Contest contest = createContest(UPCOMING_CONTEST, start, end);

            assertThat(contest.getStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        void testGetStatus_Ended() {
            Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
            Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

            Contest contest = createContest(ENDED_CONTEST, start, end);

            assertThat(contest.getStatus()).isEqualTo(EventStatus.ENDED);
        }
    }

    // ==========================================================================
    // Score Invariant Tests
    // ==========================================================================

    @Nested
    class ScoreInvariantTests {

        @Test
        void testGetScore_AlwaysWithinBounds() {
            // Test scores at various time offsets
            long[] hoursFromNow = {-48, -24, -1, 0, 1, 3, 6, 12, 24, 48, 72, 100};

            for (long hours : hoursFromNow) {
                Instant start = Instant.now().plus(hours, ChronoUnit.HOURS);
                Instant end = start.plus(24, ChronoUnit.HOURS);
                Contest contest = createContest(TEST_SIMPLE, start, end);
                int score = contest.getScore();

                assertThat(score)
                        .as("Score at %d hours should be in [%d, %d]", hours, MIN_SCORE, MAX_SCORE)
                        .isBetween(MIN_SCORE, MAX_SCORE);
            }
        }

        @Test
        void testGetScore_MonotonicallyDecreasesWithStartTime() {
            // Invariant: contests starting sooner should score >= contests starting later
            List<Integer> scores = new ArrayList<>();

            // Test from 1 hour to 100 hours in the future
            for (long hours = 1; hours <= 100; hours += 6) {
                Instant start = Instant.now().plus(hours, ChronoUnit.HOURS);
                Instant end = start.plus(24, ChronoUnit.HOURS);
                Contest contest = createContest(TEST_SIMPLE, start, end);
                scores.add(contest.getScore());
            }

            // Verify monotonic non-increasing (sooner >= later)
            for (int i = 1; i < scores.size(); i++) {
                assertThat(scores.get(i))
                        .as("Score should decrease (or stay same) as start time recedes: index %d", i)
                        .isLessThanOrEqualTo(scores.get(i - 1));
            }
        }

        @Test
        void testGetScore_CloserContestsScoreHigher() {
            // Invariant: closer start time -> higher or equal score
            Instant soonStart = Instant.now().plus(3, ChronoUnit.HOURS);
            Instant laterStart = Instant.now().plus(12, ChronoUnit.HOURS);
            Instant farStart = Instant.now().plus(48, ChronoUnit.HOURS);

            Contest soon = createContest("Soon", soonStart, soonStart.plus(24, ChronoUnit.HOURS));
            Contest later = createContest("Later", laterStart, laterStart.plus(24, ChronoUnit.HOURS));
            Contest far = createContest("Far", farStart, farStart.plus(24, ChronoUnit.HOURS));

            assertThat(soon.getScore())
                    .as("Soon >= Later")
                    .isGreaterThanOrEqualTo(later.getScore());
            assertThat(later.getScore())
                    .as("Later >= Far")
                    .isGreaterThanOrEqualTo(far.getScore());
        }
    }

    // ==========================================================================
    // Score Boundary Tests (true boundaries only)
    // ==========================================================================

    @Nested
    class ScoreBoundaryTests {

        @Test
        void testGetScore_Active_ReturnsMaxScore() {
            Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

            Contest contest = createContest(ACTIVE_CONTEST, start, end);

            assertThat(contest.getScore())
                    .as("Active contests should score maximum")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_Ended_ReturnsMinScore() {
            Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
            Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

            Contest contest = createContest(ENDED_CONTEST, start, end);

            assertThat(contest.getScore())
                    .as("Ended contests should score minimum")
                    .isEqualTo(MIN_SCORE);
        }
    }

    // ==========================================================================
    // isFavorable() Tests
    // ==========================================================================

    @Nested
    class IsFavorableTests {

        @Test
        void testIsFavorable_ActiveContest_ReturnsTrue() {
            Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

            Contest contest = createContest(ACTIVE_CONTEST, start, end);

            assertThat(contest.isFavorable()).isTrue();
        }

        @Test
        void testIsFavorable_UpcomingWithinImminentThreshold_ReturnsTrue() {
            // Within CONTEST_IMMINENT_HOURS threshold
            Instant start = Instant.now().plus(CONTEST_IMMINENT_HOURS - 3, ChronoUnit.HOURS);
            Instant end = start.plus(24, ChronoUnit.HOURS);

            Contest contest = createContest(UPCOMING_CONTEST, start, end);

            assertThat(contest.isFavorable()).isTrue();
        }

        @Test
        void testIsFavorable_UpcomingBeyondImminentThreshold_ReturnsFalse() {
            // Beyond CONTEST_IMMINENT_HOURS threshold
            Instant start = Instant.now().plus(CONTEST_IMMINENT_HOURS + 6, ChronoUnit.HOURS);
            Instant end = start.plus(24, ChronoUnit.HOURS);

            Contest contest = createContest(UPCOMING_CONTEST, start, end);

            assertThat(contest.isFavorable()).isFalse();
        }

        @Test
        void testIsFavorable_EndedContest_ReturnsFalse() {
            Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
            Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

            Contest contest = createContest(ENDED_CONTEST, start, end);

            assertThat(contest.isFavorable()).isFalse();
        }
    }

    // ==========================================================================
    // Time Remaining Tests
    // ==========================================================================

    @Nested
    class TimeRemainingTests {

        @Test
        void testGetTimeRemaining_ActiveContest_IsPositive() {
            Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

            Contest contest = createContest(ACTIVE_CONTEST, start, end);

            Duration remaining = contest.getTimeRemaining();
            assertThat(remaining.toMinutes())
                    .as("Active contest time remaining should be positive")
                    .isPositive();
        }

        @Test
        void testGetTimeRemaining_UpcomingContest_IsPositive() {
            Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
            Instant end = start.plus(24, ChronoUnit.HOURS);

            Contest contest = createContest(UPCOMING_CONTEST, start, end);

            Duration remaining = contest.getTimeRemaining();
            assertThat(remaining.toMinutes())
                    .as("Upcoming contest time remaining should be positive")
                    .isPositive();
        }

        @Test
        void testGetTimeRemaining_EndedContest_IsNegative() {
            Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
            Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

            Contest contest = createContest(ENDED_CONTEST, start, end);

            Duration remaining = contest.getTimeRemaining();
            assertThat(remaining.isNegative())
                    .as("Ended contest time remaining should be negative")
                    .isTrue();
        }

        @Test
        void testGetTimeRemainingSeconds_UpcomingContest_IsPositive() {
            Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
            Instant end = start.plus(24, ChronoUnit.HOURS);

            Contest contest = createContest(UPCOMING_CONTEST, start, end);

            long seconds = contest.getTimeRemainingSeconds();
            assertThat(seconds)
                    .as("Upcoming contest seconds remaining should be positive")
                    .isPositive();
        }
    }

    // ==========================================================================
    // isEndingSoon() Tests
    // ==========================================================================

    @Nested
    class EndingSoonTests {

        @Test
        void testIsEndingSoon_ActiveEnding_ReturnsTrue() {
            Instant start = Instant.now().minus(23, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(30, ChronoUnit.MINUTES);

            Contest contest = createContest("Ending Contest", start, end);

            assertThat(contest.isEndingSoon()).isTrue();
        }

        @Test
        void testIsEndingSoon_ActiveNotEnding_ReturnsFalse() {
            Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(2, ChronoUnit.HOURS);

            Contest contest = createContest(ACTIVE_CONTEST, start, end);

            assertThat(contest.isEndingSoon()).isFalse();
        }

        @Test
        void testIsEndingSoon_Upcoming_ReturnsFalse() {
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(24, ChronoUnit.HOURS);

            Contest contest = createContest(UPCOMING_CONTEST, start, end);

            assertThat(contest.isEndingSoon())
                    .as("Upcoming contests are never ending soon")
                    .isFalse();
        }
    }

    // ==========================================================================
    // Defensive Copy Tests
    // ==========================================================================

    @Nested
    class DefensiveCopyTests {

        @Test
        void testDefensivelyCopies_Bands() {
            Set<FrequencyBand> mutableBands = java.util.EnumSet.noneOf(FrequencyBand.class);
            mutableBands.add(FrequencyBand.BAND_20M);

            Contest contest = new Contest(
                    TEST_CONTEST,
                    Instant.now(),
                    Instant.now().plus(24, ChronoUnit.HOURS),
                    mutableBands,
                    Set.of(MODE_CW),
                    ORGANIZER_ARRL,
                    URL_EXAMPLE,
                    URL_EXAMPLE_RULES
            );

            // Modify the original set
            mutableBands.add(FrequencyBand.BAND_40M);

            // Contest's bands should not be affected
            assertThat(contest.bands()).hasSize(1);
            assertThat(contest.bands()).contains(FrequencyBand.BAND_20M);
        }

        @Test
        void testDefensivelyCopies_Modes() {
            Set<String> mutableModes = new java.util.HashSet<>();
            mutableModes.add(MODE_CW);

            Contest contest = new Contest(
                    TEST_CONTEST,
                    Instant.now(),
                    Instant.now().plus(24, ChronoUnit.HOURS),
                    Set.of(FrequencyBand.BAND_20M),
                    mutableModes,
                    ORGANIZER_ARRL,
                    URL_EXAMPLE,
                    URL_EXAMPLE_RULES
            );

            // Modify the original set
            mutableModes.add("SSB");

            // Contest's modes should not be affected
            assertThat(contest.modes()).hasSize(1);
            assertThat(contest.modes()).contains(MODE_CW);
        }

        @Test
        void testConvertsNullTo_EmptySets() {
            Contest contest = new Contest(
                    TEST_CONTEST,
                    Instant.now(),
                    Instant.now().plus(24, ChronoUnit.HOURS),
                    null,
                    null,
                    ORGANIZER_ARRL,
                    URL_EXAMPLE,
                    URL_EXAMPLE_RULES
            );

            assertThat(contest.bands()).isNotNull().isEmpty();
            assertThat(contest.modes()).isNotNull().isEmpty();
        }
    }

    // ==========================================================================
    // Accessor Tests
    // ==========================================================================

    @Nested
    class AccessorTests {

        @Test
        void testGetName_ReturnsContestName() {
            Contest contest = createContest("Test Contest Name", Instant.now(),
                    Instant.now().plus(1, ChronoUnit.HOURS));

            assertThat(contest.getName()).isEqualTo("Test Contest Name");
            assertThat(contest.name()).isEqualTo("Test Contest Name");
        }

        @Test
        void testGetStartTime_ReturnsStartInstant() {
            Instant start = Instant.now();
            Contest contest = createContest(TEST_SIMPLE, start, start.plus(24, ChronoUnit.HOURS));

            assertThat(contest.getStartTime()).isEqualTo(start);
            assertThat(contest.startTime()).isEqualTo(start);
        }

        @Test
        void testGetEndTime_ReturnsEndInstant() {
            Instant start = Instant.now();
            Instant end = start.plus(24, ChronoUnit.HOURS);
            Contest contest = createContest(TEST_SIMPLE, start, end);

            assertThat(contest.getEndTime()).isEqualTo(end);
            assertThat(contest.endTime()).isEqualTo(end);
        }
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    /**
     * Helper method to create a Contest with minimal fields.
     */
    private Contest createContest(String name, Instant start, Instant end) {
        return new Contest(
                name,
                start,
                end,
                Set.of(),
                Set.of(),
                null,
                null,
                null
        );
    }
}
