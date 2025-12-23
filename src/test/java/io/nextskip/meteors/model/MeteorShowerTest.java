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
}
