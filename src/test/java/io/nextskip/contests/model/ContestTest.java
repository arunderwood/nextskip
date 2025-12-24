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
class ContestTest {

    @Test
    void testActiveContest_Status() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest("Active Contest", start, end);

        assertEquals(EventStatus.ACTIVE, contest.getStatus());
    }

    @Test
    void testUpcomingContest_Status() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        assertEquals(EventStatus.UPCOMING, contest.getStatus());
    }

    @Test
    void testEndedContest_Status() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest("Ended Contest", start, end);

        assertEquals(EventStatus.ENDED, contest.getStatus());
    }

    @Test
    void testActiveContest_Score() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest("Active Contest", start, end);

        // Active contests always score 100
        assertEquals(100, contest.getScore());
    }

    @Test
    void testUpcomingContest_Score_Within6Hours() {
        // Contest starting in 3 hours
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(27, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        // Score should be between 80-100 (linear decay)
        int score = contest.getScore();
        assertTrue(score >= 80 && score <= 100, "Score should be 80-100 for <6h, was: " + score);
    }

    @Test
    void testUpcomingContest_Score_Within24Hours() {
        // Contest starting in 12 hours
        Instant start = Instant.now().plus(12, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(36, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        // Score should be between 40-80 (linear decay)
        int score = contest.getScore();
        assertTrue(score >= 40 && score <= 80, "Score should be 40-80 for 6-24h, was: " + score);
    }

    @Test
    void testUpcomingContest_Score_Within72Hours() {
        // Contest starting in 48 hours
        Instant start = Instant.now().plus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(72, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        // Score should be between 20-40 (linear decay)
        int score = contest.getScore();
        assertTrue(score >= 20 && score <= 40, "Score should be 20-40 for 24-72h, was: " + score);
    }

    @Test
    void testUpcomingContest_Score_Beyond72Hours() {
        // Contest starting in 100 hours
        Instant start = Instant.now().plus(100, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(124, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        // Score should be 10 (far future)
        assertEquals(10, contest.getScore());
    }

    @Test
    void testEndedContest_Score() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest("Ended Contest", start, end);

        // Ended contests score 0
        assertEquals(0, contest.getScore());
    }

    @Test
    void testActiveContest_IsFavorable() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest("Active Contest", start, end);

        assertTrue(contest.isFavorable());
    }

    @Test
    void testUpcomingContest_IsFavorable_Within6Hours() {
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(27, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        assertTrue(contest.isFavorable());
    }

    @Test
    void testUpcomingContest_NotFavorable_Beyond6Hours() {
        Instant start = Instant.now().plus(12, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(36, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        assertFalse(contest.isFavorable());
    }

    @Test
    void testEndedContest_NotFavorable() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest("Ended Contest", start, end);

        assertFalse(contest.isFavorable());
    }

    @Test
    void testActiveContest_TimeRemaining() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Contest contest = createContest("Active Contest", start, end);

        Duration remaining = contest.getTimeRemaining();
        // Time remaining is to end time for active contests
        assertTrue(remaining.toMinutes() > 0 && remaining.toMinutes() <= 60);
    }

    @Test
    void testUpcomingContest_TimeRemaining() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        Duration remaining = contest.getTimeRemaining();
        // Time remaining is to start time for upcoming contests
        assertTrue(remaining.toMinutes() > 60 && remaining.toMinutes() <= 120);
    }

    @Test
    void testEndedContest_TimeRemaining() {
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(24, ChronoUnit.HOURS);

        Contest contest = createContest("Ended Contest", start, end);

        Duration remaining = contest.getTimeRemaining();
        // Time remaining is negative for ended contests
        assertTrue(remaining.isNegative());
    }

    @Test
    void testTimeRemainingSeconds() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(26, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        long seconds = contest.getTimeRemainingSeconds();
        // Should be around 7200 seconds (2 hours)
        assertTrue(seconds > 3600 && seconds <= 7200);
    }

    @Test
    void testIsEndingSoon_ActiveEnding() {
        Instant start = Instant.now().minus(23, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(30, ChronoUnit.MINUTES);

        Contest contest = createContest("Ending Contest", start, end);

        assertTrue(contest.isEndingSoon());
    }

    @Test
    void testIsEndingSoon_ActiveNotEnding() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(2, ChronoUnit.HOURS);

        Contest contest = createContest("Active Contest", start, end);

        assertFalse(contest.isEndingSoon());
    }

    @Test
    void testIsEndingSoon_UpcomingNeverEnding() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(25, ChronoUnit.HOURS);

        Contest contest = createContest("Upcoming Contest", start, end);

        // Upcoming contests are never "ending soon"
        assertFalse(contest.isEndingSoon());
    }

    @Test
    void testDefensiveCopyBands() {
        Set<FrequencyBand> mutableBands = new java.util.HashSet<>();
        mutableBands.add(FrequencyBand.BAND_20M);

        Contest contest = new Contest(
                "Test Contest",
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                mutableBands,
                Set.of("CW"),
                "ARRL",
                "https://example.com",
                "https://example.com/rules"
        );

        // Modify the original set
        mutableBands.add(FrequencyBand.BAND_40M);

        // Contest's bands should not be affected (defensive copy)
        assertEquals(1, contest.bands().size());
        assertTrue(contest.bands().contains(FrequencyBand.BAND_20M));
    }

    @Test
    void testDefensiveCopyModes() {
        Set<String> mutableModes = new java.util.HashSet<>();
        mutableModes.add("CW");

        Contest contest = new Contest(
                "Test Contest",
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Set.of(FrequencyBand.BAND_20M),
                mutableModes,
                "ARRL",
                "https://example.com",
                "https://example.com/rules"
        );

        // Modify the original set
        mutableModes.add("SSB");

        // Contest's modes should not be affected (defensive copy)
        assertEquals(1, contest.modes().size());
        assertTrue(contest.modes().contains("CW"));
    }

    @Test
    void testNullBandsBecomesEmptySet() {
        Contest contest = new Contest(
                "Test Contest",
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                null,
                Set.of("CW"),
                "ARRL",
                "https://example.com",
                "https://example.com/rules"
        );

        assertNotNull(contest.bands());
        assertTrue(contest.bands().isEmpty());
    }

    @Test
    void testNullModesBecomesEmptySet() {
        Contest contest = new Contest(
                "Test Contest",
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Set.of(FrequencyBand.BAND_20M),
                null,
                "ARRL",
                "https://example.com",
                "https://example.com/rules"
        );

        assertNotNull(contest.modes());
        assertTrue(contest.modes().isEmpty());
    }

    @Test
    void testGetName() {
        Contest contest = createContest("Test Contest Name", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
        assertEquals("Test Contest Name", contest.getName());
        assertEquals("Test Contest Name", contest.name());
    }

    @Test
    void testGetStartTime() {
        Instant start = Instant.now();
        Contest contest = createContest("Test", start, start.plus(24, ChronoUnit.HOURS));
        assertEquals(start, contest.getStartTime());
        assertEquals(start, contest.startTime());
    }

    @Test
    void testGetEndTime() {
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
