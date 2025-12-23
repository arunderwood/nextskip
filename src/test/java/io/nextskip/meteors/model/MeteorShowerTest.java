package io.nextskip.meteors.model;

import io.nextskip.common.model.EventStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for MeteorShower model.
 */
class MeteorShowerTest {

    // Test data builders
    private static MeteorShower createActiveAtPeak() {
        Instant now = Instant.now();
        return new MeteorShower(
                "Perseids 2025",
                "PER",
                now.minus(Duration.ofHours(6)),  // Peak started 6 hours ago
                now.plus(Duration.ofHours(18)),  // Peak ends in 18 hours
                now.minus(Duration.ofDays(5)),   // Visibility started 5 days ago
                now.plus(Duration.ofDays(3)),    // Visibility ends in 3 days
                100,
                "109P/Swift-Tuttle",
                "https://imo.net"
        );
    }

    private static MeteorShower createActiveNotAtPeak() {
        Instant now = Instant.now();
        return new MeteorShower(
                "Perseids 2025",
                "PER",
                now.plus(Duration.ofDays(2)),    // Peak starts in 2 days
                now.plus(Duration.ofDays(2)).plus(Duration.ofHours(36)), // Peak duration
                now.minus(Duration.ofDays(1)),   // Visibility started yesterday
                now.plus(Duration.ofDays(5)),    // Visibility ends in 5 days
                100,
                "109P/Swift-Tuttle",
                "https://imo.net"
        );
    }

    private static MeteorShower createUpcoming() {
        Instant now = Instant.now();
        return new MeteorShower(
                "Geminids 2025",
                "GEM",
                now.plus(Duration.ofDays(10)),
                now.plus(Duration.ofDays(10)).plus(Duration.ofHours(24)),
                now.plus(Duration.ofDays(7)),
                now.plus(Duration.ofDays(13)),
                150,
                "3200 Phaethon",
                "https://imo.net"
        );
    }

    private static MeteorShower createEnded() {
        Instant now = Instant.now();
        return new MeteorShower(
                "Quadrantids 2025",
                "QUA",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(9)),
                now.minus(Duration.ofDays(13)),
                now.minus(Duration.ofDays(7)),
                110,
                "2003 EH1",
                "https://imo.net"
        );
    }

    @Nested
    class StatusTests {
        @Test
        void activeAtPeak_returnsActive() {
            MeteorShower shower = createActiveAtPeak();
            assertEquals(EventStatus.ACTIVE, shower.getStatus());
        }

        @Test
        void activeNotAtPeak_returnsActive() {
            MeteorShower shower = createActiveNotAtPeak();
            assertEquals(EventStatus.ACTIVE, shower.getStatus());
        }

        @Test
        void upcoming_returnsUpcoming() {
            MeteorShower shower = createUpcoming();
            assertEquals(EventStatus.UPCOMING, shower.getStatus());
        }

        @Test
        void ended_returnsEnded() {
            MeteorShower shower = createEnded();
            assertEquals(EventStatus.ENDED, shower.getStatus());
        }
    }

    @Nested
    class ScoreTests {
        @Test
        void activeAtPeak_scoresHigh() {
            MeteorShower shower = createActiveAtPeak();
            int score = shower.getScore();
            assertTrue(score >= 85, "Active at peak should score >= 85, got " + score);
            assertTrue(score <= 100, "Score should be <= 100, got " + score);
        }

        @Test
        void activeNotAtPeak_scoresMedium() {
            MeteorShower shower = createActiveNotAtPeak();
            int score = shower.getScore();
            assertTrue(score >= 40, "Active not at peak should score >= 40, got " + score);
            assertTrue(score < 85, "Active not at peak should score < 85, got " + score);
        }

        @Test
        void upcomingSoon_scoresMediumHigh() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.plus(Duration.ofHours(12)),
                    now.plus(Duration.ofHours(36)),
                    now.plus(Duration.ofHours(6)), // Starts in 6 hours
                    now.plus(Duration.ofDays(2)),
                    50, null, null
            );
            int score = shower.getScore();
            assertTrue(score >= 60, "Upcoming in 6 hours should score >= 60, got " + score);
        }

        @Test
        void ended_scoresZero() {
            MeteorShower shower = createEnded();
            assertEquals(0, shower.getScore());
        }
    }

    @Nested
    class ZhrCalculationTests {
        @Test
        void atPeak_returnsNearPeakZhr() {
            MeteorShower shower = createActiveAtPeak();
            int currentZhr = shower.getCurrentZhr();
            // Should be close to peak (accounting for Gaussian decay from midpoint)
            assertTrue(currentZhr > shower.peakZhr() * 0.7,
                    "At peak should return near peak ZHR, got " + currentZhr);
        }

        @Test
        void activeButFarFromPeak_returnsDecayedZhr() {
            MeteorShower shower = createActiveNotAtPeak();
            int currentZhr = shower.getCurrentZhr();
            assertTrue(currentZhr > 0, "Active shower should have ZHR > 0");
            assertTrue(currentZhr < shower.peakZhr(),
                    "Far from peak should have ZHR < peak");
        }

        @Test
        void ended_returnsZero() {
            MeteorShower shower = createEnded();
            assertEquals(0, shower.getCurrentZhr());
        }
    }

    @Nested
    class FavorableTests {
        @Test
        void atPeak_isFavorable() {
            MeteorShower shower = createActiveAtPeak();
            assertTrue(shower.isFavorable());
        }

        @Test
        void ended_isNotFavorable() {
            MeteorShower shower = createEnded();
            assertFalse(shower.isFavorable());
        }
    }

    @Nested
    class PeakTests {
        @Test
        void atPeak_returnsTrue() {
            MeteorShower shower = createActiveAtPeak();
            assertTrue(shower.isAtPeak());
        }

        @Test
        void notAtPeak_returnsFalse() {
            MeteorShower shower = createActiveNotAtPeak();
            assertFalse(shower.isAtPeak());
        }
    }

    @Nested
    class TimeRemainingTests {
        @Test
        void upcoming_returnsTimeToStart() {
            MeteorShower shower = createUpcoming();
            Duration remaining = shower.getTimeRemaining();
            assertTrue(remaining.toHours() > 0, "Upcoming should have positive time remaining");
        }

        @Test
        void active_returnsTimeToEnd() {
            MeteorShower shower = createActiveAtPeak();
            Duration remaining = shower.getTimeRemaining();
            assertTrue(remaining.toHours() > 0, "Active should have time remaining to end");
        }

        @Test
        void ended_returnsNegativeTime() {
            MeteorShower shower = createEnded();
            Duration remaining = shower.getTimeRemaining();
            assertTrue(remaining.isNegative(), "Ended should have negative time remaining");
        }

        @Test
        void timeRemainingSeconds_convertsToSeconds() {
            MeteorShower shower = createUpcoming();
            long seconds = shower.getTimeRemainingSeconds();
            assertEquals(shower.getTimeRemaining().getSeconds(), seconds);
        }
    }

    @Nested
    class EndingSoonTests {
        @Test
        void peakEndingInFiveHours_isEndingSoon() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.minus(Duration.ofHours(20)),
                    now.plus(Duration.ofHours(5)),  // Peak ends in 5 hours
                    now.minus(Duration.ofDays(1)),
                    now.plus(Duration.ofDays(1)),
                    50, null, null
            );
            assertTrue(shower.isEndingSoon(), "Shower with peak ending in 5 hours should be ending soon");
        }

        @Test
        void peakEndingInSevenHours_notEndingSoon() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.minus(Duration.ofHours(12)),
                    now.plus(Duration.ofHours(7)),  // Peak ends in 7 hours
                    now.minus(Duration.ofDays(1)),
                    now.plus(Duration.ofDays(1)),
                    50, null, null
            );
            assertFalse(shower.isEndingSoon(), "Shower with peak ending in 7 hours should not be ending soon");
        }

        @Test
        void upcoming_notEndingSoon() {
            MeteorShower shower = createUpcoming();
            assertFalse(shower.isEndingSoon(), "Upcoming shower should not be ending soon");
        }

        @Test
        void ended_notEndingSoon() {
            MeteorShower shower = createEnded();
            assertFalse(shower.isEndingSoon(), "Ended shower should not be ending soon");
        }
    }

    @Nested
    class AdditionalFavorableTests {
        @Test
        void activeNearPeak_isFavorable() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.plus(Duration.ofHours(10)),  // Peak starts in 10 hours
                    now.plus(Duration.ofHours(34)),
                    now.minus(Duration.ofHours(12)), // Visibility started 12 hours ago
                    now.plus(Duration.ofDays(2)),
                    50, null, null
            );
            assertTrue(shower.isFavorable(), "Active shower with peak in 10 hours should be favorable");
        }

        @Test
        void upcomingWithinTwelveHours_isFavorable() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.plus(Duration.ofHours(18)),
                    now.plus(Duration.ofHours(42)),
                    now.plus(Duration.ofHours(8)),  // Starts in 8 hours
                    now.plus(Duration.ofDays(2)),
                    50, null, null
            );
            assertTrue(shower.isFavorable(), "Upcoming shower within 12 hours should be favorable");
        }

        @Test
        void activeFarFromPeak_isNotFavorable() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.plus(Duration.ofDays(3)),  // Peak starts in 3 days
                    now.plus(Duration.ofDays(4)),
                    now.minus(Duration.ofHours(1)), // Visibility started 1 hour ago
                    now.plus(Duration.ofDays(5)),
                    50, null, null
            );
            assertFalse(shower.isFavorable(), "Active shower with peak in 3 days should not be favorable");
        }
    }

    @Nested
    class AdditionalScoreTests {
        @Test
        void upcoming24To72Hours_scoresCorrectly() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.plus(Duration.ofHours(60)),  // ~2.5 days
                    now.plus(Duration.ofHours(84)),
                    now.plus(Duration.ofHours(48)),  // Starts in 48 hours
                    now.plus(Duration.ofDays(4)),
                    50, null, null
            );
            int score = shower.getScore();
            assertTrue(score >= 30 && score <= 60, "Upcoming 48 hours should score 30-60, got " + score);
        }

        @Test
        void upcomingOver72Hours_scoresLow() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.plus(Duration.ofDays(6)),
                    now.plus(Duration.ofDays(7)),
                    now.plus(Duration.ofDays(5)),  // Starts in 5 days (120 hours)
                    now.plus(Duration.ofDays(8)),
                    50, null, null
            );
            assertEquals(15, shower.getScore(), "Upcoming 120 hours should score 15");
        }

        @Test
        void highZhrPeak_scoresMaximum() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    "Geminids", "GEM",
                    now.minus(Duration.ofHours(6)),
                    now.plus(Duration.ofHours(18)),
                    now.minus(Duration.ofDays(1)),
                    now.plus(Duration.ofDays(2)),
                    150,  // Very high ZHR
                    "3200 Phaethon", null
            );
            int score = shower.getScore();
            assertEquals(100, score, "High ZHR at peak should score 100");
        }
    }

    @Nested
    class AdditionalZhrTests {
        @Test
        void upcomingShower_returnsMinimalZhr() {
            MeteorShower shower = createUpcoming();
            assertEquals(1, shower.getCurrentZhr(), "Upcoming shower should return ZHR of 1");
        }

        @Test
        void gaussianDecay_calculatesCorrectly() {
            Instant now = Instant.now();
            // Create shower with peak exactly at current time
            MeteorShower shower = new MeteorShower(
                    "Test", "TST",
                    now.minus(Duration.ofHours(12)),
                    now.plus(Duration.ofHours(12)),  // Peak midpoint is now
                    now.minus(Duration.ofDays(1)),
                    now.plus(Duration.ofDays(1)),
                    100, null, null
            );
            int currentZhr = shower.getCurrentZhr();
            // At peak midpoint, Gaussian decay should give ~100
            assertTrue(currentZhr >= 95, "At peak midpoint should be near maximum ZHR, got " + currentZhr);
        }
    }

    @Nested
    class TimeToPeakTests {
        @Test
        void beforePeak_returnsPositiveDuration() {
            MeteorShower shower = createActiveNotAtPeak();
            Duration timeToPeak = shower.getTimeToPeak();
            assertTrue(timeToPeak.toHours() > 0, "Before peak should return positive duration");
        }

        @Test
        void atPeak_returnsZero() {
            MeteorShower shower = createActiveAtPeak();
            Duration timeToPeak = shower.getTimeToPeak();
            assertEquals(Duration.ZERO, timeToPeak, "At peak should return ZERO duration");
        }

        @Test
        void afterPeak_returnsNegativeDuration() {
            MeteorShower shower = createEnded();
            Duration timeToPeak = shower.getTimeToPeak();
            assertTrue(timeToPeak.isNegative(), "After peak should return negative duration");
        }

        @Test
        void timeToPeakSeconds_convertsToSeconds() {
            MeteorShower shower = createActiveNotAtPeak();
            long seconds = shower.getTimeToPeakSeconds();
            assertEquals(shower.getTimeToPeak().getSeconds(), seconds);
        }
    }

    @Nested
    class EventInterfaceTests {
        @Test
        void getName_returnsCorrectName() {
            MeteorShower shower = createActiveAtPeak();
            assertEquals("Perseids 2025", shower.getName());
        }

        @Test
        void getStartTime_returnsVisibilityStart() {
            MeteorShower shower = createActiveAtPeak();
            assertEquals(shower.visibilityStart(), shower.getStartTime());
        }

        @Test
        void getEndTime_returnsVisibilityEnd() {
            MeteorShower shower = createActiveAtPeak();
            assertEquals(shower.visibilityEnd(), shower.getEndTime());
        }
    }
}
