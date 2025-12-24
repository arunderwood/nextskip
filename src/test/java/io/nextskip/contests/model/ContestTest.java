package io.nextskip.contests.model;

import io.nextskip.common.model.EventStatus;
import io.nextskip.common.model.FrequencyBand;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Contest model.
 */
@SuppressWarnings("PMD.TooManyMethods") // Model class with many behaviors requires comprehensive test methods
class ContestTest {

    private static final String TEST_CONTEST = "Test Contest";
    private static final String MODE_CW = "CW";
    private static final String ORGANIZER_ARRL = "ARRL";
    private static final String URL_EXAMPLE = "https://example.com";
    private static final String URL_EXAMPLE_RULES = "https://example.com/rules";
    private static final String UPCOMING_CONTEST = "Upcoming Contest";
    private static final String ACTIVE_CONTEST = "Active Contest";
    private static final String ENDED_CONTEST = "Ended Contest";

    @Test
    void shouldBeActive_Status() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest(ACTIVE_CONTEST, start, end);

        assertEquals(EventStatus.ACTIVE, contest.getStatus());
    }

    @Test
    void shouldBeUpcoming_Status() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        assertEquals(EventStatus.UPCOMING, contest.getStatus());
    }

    @Test
    void shouldBeEnded_Status() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest(ENDED_CONTEST, start, end);

        assertEquals(EventStatus.ENDED, contest.getStatus());
    }

    @Test
    void shouldScore_ActiveContest() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest(ACTIVE_CONTEST, start, end);

        // Active contests always score 100
        assertEquals(100, contest.getScore());
    }

    @Test
    void shouldScore_UpcomingWithin6Hours() {
        // Contest starting in 3 hours
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(27, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        // Score should be between 80-100 (linear decay)
        int score = contest.getScore();
        assertTrue(score >= 80 && score <= 100, "Score should be 80-100 for <6h, was: " + score);
    }

    @Test
    void shouldScore_UpcomingWithin24Hours() {
        // Contest starting in 12 hours
        Instant start = Instant.now().plus(12, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(36, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        // Score should be between 40-80 (linear decay)
        int score = contest.getScore();
        assertTrue(score >= 40 && score <= 80, "Score should be 40-80 for 6-24h, was: " + score);
    }

    @Test
    void shouldScore_UpcomingWithin72Hours() {
        // Contest starting in 48 hours
        Instant start = Instant.now().plus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(72, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        // Score should be between 20-40 (linear decay)
        int score = contest.getScore();
        assertTrue(score >= 20 && score <= 40, "Score should be 20-40 for 24-72h, was: " + score);
    }

    @Test
    void shouldScore_UpcomingBeyond72Hours() {
        // Contest starting in 100 hours
        Instant start = Instant.now().plus(100, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(124, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        // Score should be 10 (far future)
        assertEquals(10, contest.getScore());
    }

    @Test
    void shouldScore_EndedContest() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest(ENDED_CONTEST, start, end);

        // Ended contests score 0
        assertEquals(0, contest.getScore());
    }

    @Test
    void shouldBeFavorable_ActiveContest() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest(ACTIVE_CONTEST, start, end);

        assertTrue(contest.isFavorable());
    }

    @Test
    void shouldBeFavorable_UpcomingWithin6Hours() {
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(27, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        assertTrue(contest.isFavorable());
    }

    @Test
    void shouldNotBeFavorable_UpcomingBeyond6Hours() {
        Instant start = Instant.now().plus(12, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(36, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        assertFalse(contest.isFavorable());
    }

    @Test
    void shouldNotBeFavorable_EndedContest() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest(ENDED_CONTEST, start, end);

        assertFalse(contest.isFavorable());
    }

    @Test
    void shouldReturnTimeRemaining_ActiveContest() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest(ACTIVE_CONTEST, start, end);

        Duration remaining = contest.getTimeRemaining();
        // Time remaining is to end time for active contests
        assertTrue(remaining.toMinutes() > 0 && remaining.toMinutes() <= 60);
    }

    @Test
    void shouldReturnTimeRemaining_UpcomingContest() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        Duration remaining = contest.getTimeRemaining();
        // Time remaining is to start time for upcoming contests
        assertTrue(remaining.toMinutes() > 60 && remaining.toMinutes() <= 120);
    }

    @Test
    void shouldReturnTimeRemaining_EndedContest() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest(ENDED_CONTEST, start, end);

        Duration remaining = contest.getTimeRemaining();
        // Time remaining is negative for ended contests
        assertTrue(remaining.isNegative());
    }

    @Test
    void shouldReturn_TimeRemainingSeconds() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        long seconds = contest.getTimeRemainingSeconds();
        // Should be around 7200 seconds (2 hours)
        assertTrue(seconds > 3600 && seconds <= 7200);
    }

    @Test
    void shouldBeEndingSoon_ActiveEnding() {
        Instant start = Instant.now().minus(23, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(30, ChronoUnit.MINUTES);

        Contest contest = createContest("Ending Contest", start, end);

        assertTrue(contest.isEndingSoon());
    }

    @Test
    void shouldNotBeEndingSoon_ActiveNotEnding() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(2, ChronoUnit.HOURS);

        Contest contest = createContest(ACTIVE_CONTEST, start, end);

        assertFalse(contest.isEndingSoon());
    }

    @Test
    void shouldNotBeEndingSoon_UpcomingNeverEnding() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(25, ChronoUnit.HOURS);

        Contest contest = createContest(UPCOMING_CONTEST, start, end);

        // Upcoming contests are never "ending soon"
        assertFalse(contest.isEndingSoon());
    }

    @Test
    void shouldDefensivelyCopy_Bands() {
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

        // Contest's bands should not be affected (defensive copy)
        assertEquals(1, contest.bands().size());
        assertTrue(contest.bands().contains(FrequencyBand.BAND_20M));
    }

    @Test
    void shouldDefensivelyCopy_Modes() {
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

        // Contest's modes should not be affected (defensive copy)
        assertEquals(1, contest.modes().size());
        assertTrue(contest.modes().contains(MODE_CW));
    }

    @Test
    void shouldConvertNullTo_EmptyBandsSet() {
        Contest contest = new Contest(
                TEST_CONTEST,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                null,
                Set.of(MODE_CW),
                ORGANIZER_ARRL,
                URL_EXAMPLE,
                URL_EXAMPLE_RULES
        );

        assertNotNull(contest.bands());
        assertTrue(contest.bands().isEmpty());
    }

    @Test
    void shouldConvertNullTo_EmptyModesSet() {
        Contest contest = new Contest(
                TEST_CONTEST,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Set.of(FrequencyBand.BAND_20M),
                null,
                ORGANIZER_ARRL,
                URL_EXAMPLE,
                URL_EXAMPLE_RULES
        );

        assertNotNull(contest.modes());
        assertTrue(contest.modes().isEmpty());
    }

    @Test
    void shouldReturn_Name() {
        Contest contest = createContest("Test Contest Name", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
        assertEquals("Test Contest Name", contest.getName());
        assertEquals("Test Contest Name", contest.name());
    }

    @Test
    void shouldReturn_StartTime() {
        Instant start = Instant.now();
        Contest contest = createContest("Test", start, start.plus(24, ChronoUnit.HOURS));
        assertEquals(start, contest.getStartTime());
        assertEquals(start, contest.startTime());
    }

    @Test
    void shouldReturn_EndTime() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);
        Contest contest = createContest("Test", start, end);
        assertEquals(end, contest.getEndTime());
        assertEquals(end, contest.endTime());
    }

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
