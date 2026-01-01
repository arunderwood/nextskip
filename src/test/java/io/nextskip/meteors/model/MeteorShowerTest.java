package io.nextskip.meteors.model;

import io.nextskip.common.model.EventStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static io.nextskip.test.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MeteorShower model.
 *
 * <p>Tests use invariant-based assertions to verify scoring contracts
 * without coupling to specific algorithm coefficients.
 */
class MeteorShowerTest {

    private static final String PERSEIDS_2025 = "Perseids 2025";
    private static final String SWIFT_TUTTLE = "109P/Swift-Tuttle";
    private static final String IMO_URL = "https://imo.net";
    private static final String TEST_SHOWER = "Test";
    private static final String TEST_CODE = "TST";

    // =========================================================================
    // Test Data Builders
    // =========================================================================

    private static MeteorShower createActiveAtPeak() {
        Instant now = Instant.now();
        return new MeteorShower(
                PERSEIDS_2025,
                "PER",
                now.minus(Duration.ofHours(6)),  // Peak started 6 hours ago
                now.plus(Duration.ofHours(18)),  // Peak ends in 18 hours
                now.minus(Duration.ofDays(5)),   // Visibility started 5 days ago
                now.plus(Duration.ofDays(3)),    // Visibility ends in 3 days
                DEFAULT_PEAK_ZHR,
                SWIFT_TUTTLE,
                IMO_URL
        );
    }

    private static MeteorShower createActiveNotAtPeak() {
        Instant now = Instant.now();
        return new MeteorShower(
                PERSEIDS_2025,
                "PER",
                now.plus(Duration.ofDays(2)),    // Peak starts in 2 days
                now.plus(Duration.ofDays(2)).plus(Duration.ofHours(36)), // Peak duration
                now.minus(Duration.ofDays(1)),   // Visibility started yesterday
                now.plus(Duration.ofDays(5)),    // Visibility ends in 5 days
                DEFAULT_PEAK_ZHR,
                SWIFT_TUTTLE,
                IMO_URL
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
                IMO_URL
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
                IMO_URL
        );
    }

    // =========================================================================
    // Status Tests
    // =========================================================================

    @Nested
    class StatusTests {
        @Test
        void testGetStatus_ActiveAtPeak() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.getStatus()).isEqualTo(EventStatus.ACTIVE);
        }

        @Test
        void testGetStatus_ActiveNotAtPeak() {
            MeteorShower shower = createActiveNotAtPeak();
            assertThat(shower.getStatus()).isEqualTo(EventStatus.ACTIVE);
        }

        @Test
        void testGetStatus_Upcoming() {
            MeteorShower shower = createUpcoming();
            assertThat(shower.getStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        void testGetStatus_Ended() {
            MeteorShower shower = createEnded();
            assertThat(shower.getStatus()).isEqualTo(EventStatus.ENDED);
        }
    }

    // =========================================================================
    // Score Invariant Tests
    // =========================================================================

    @Nested
    class ScoreInvariantTests {

        @Test
        void testGetScore_AlwaysWithinBounds() {
            // Test all standard shower states
            MeteorShower[] showers = {
                    createActiveAtPeak(),
                    createActiveNotAtPeak(),
                    createUpcoming(),
                    createEnded()
            };

            for (MeteorShower shower : showers) {
                int score = shower.getScore();
                assertThat(score)
                        .as("Score for %s should be in [%d, %d]", shower.name(), MIN_SCORE, MAX_SCORE)
                        .isBetween(MIN_SCORE, MAX_SCORE);
            }
        }

        @Test
        void testGetScore_PeakScoresHigherThanNonPeak() {
            // Invariant: At peak should score higher than not at peak
            MeteorShower atPeak = createActiveAtPeak();
            MeteorShower notAtPeak = createActiveNotAtPeak();

            assertThat(atPeak.getScore())
                    .as("At peak should score higher than not at peak")
                    .isGreaterThan(notAtPeak.getScore());
        }

        @Test
        void testGetScore_ActiveScoresHigherThanUpcoming() {
            // Invariant: Active showers should generally score higher than upcoming
            MeteorShower atPeak = createActiveAtPeak();
            MeteorShower upcoming = createUpcoming();

            assertThat(atPeak.getScore())
                    .as("Active at peak should score higher than upcoming")
                    .isGreaterThan(upcoming.getScore());
        }

        @Test
        void testGetScore_UpcomingScoresHigherThanEnded() {
            // Invariant: Upcoming showers should score higher than ended
            MeteorShower upcoming = createUpcoming();
            MeteorShower ended = createEnded();

            assertThat(upcoming.getScore())
                    .as("Upcoming should score higher than ended")
                    .isGreaterThan(ended.getScore());
        }
    }

    // =========================================================================
    // Score Boundary Tests
    // =========================================================================

    @Nested
    class ScoreBoundaryTests {

        @Test
        void testGetScore_Ended_ReturnsMinScore() {
            MeteorShower shower = createEnded();
            assertThat(shower.getScore())
                    .as("Ended shower should score minimum")
                    .isEqualTo(MIN_SCORE);
        }

        @Test
        void testGetScore_ActiveAtPeak_ScoresHigh() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.getScore())
                    .as("Active at peak should score near maximum")
                    .isGreaterThanOrEqualTo(HOT_THRESHOLD);
        }
    }

    // =========================================================================
    // ZHR Calculation Invariant Tests
    // =========================================================================

    @Nested
    class ZhrInvariantTests {

        @Test
        void testGetCurrentZhr_AlwaysWithinBounds() {
            MeteorShower[] showers = {
                    createActiveAtPeak(),
                    createActiveNotAtPeak(),
                    createUpcoming(),
                    createEnded()
            };

            for (MeteorShower shower : showers) {
                int currentZhr = shower.getCurrentZhr();
                assertThat(currentZhr)
                        .as("Current ZHR for %s should be >= 0", shower.name())
                        .isGreaterThanOrEqualTo(0);
                assertThat(currentZhr)
                        .as("Current ZHR for %s should be <= peak ZHR", shower.name())
                        .isLessThanOrEqualTo(shower.peakZhr());
            }
        }

        @Test
        void testGetCurrentZhr_AtPeakHigherThanNotAtPeak() {
            MeteorShower atPeak = createActiveAtPeak();
            MeteorShower notAtPeak = createActiveNotAtPeak();

            assertThat(atPeak.getCurrentZhr())
                    .as("At peak ZHR should be >= not at peak ZHR")
                    .isGreaterThanOrEqualTo(notAtPeak.getCurrentZhr());
        }

        @Test
        void testGetCurrentZhr_ActiveHasPositiveZhr() {
            MeteorShower activeAtPeak = createActiveAtPeak();
            MeteorShower activeNotAtPeak = createActiveNotAtPeak();

            assertThat(activeAtPeak.getCurrentZhr())
                    .as("Active at peak should have ZHR > 0")
                    .isPositive();
            assertThat(activeNotAtPeak.getCurrentZhr())
                    .as("Active not at peak should have ZHR > 0")
                    .isPositive();
        }

        @Test
        void testGetCurrentZhr_EndedReturnsZero() {
            MeteorShower shower = createEnded();
            assertThat(shower.getCurrentZhr())
                    .as("Ended shower should have zero ZHR")
                    .isZero();
        }

        @Test
        void testGetCurrentZhr_UpcomingReturnsMinimal() {
            MeteorShower shower = createUpcoming();
            int currentZhr = shower.getCurrentZhr();
            // Upcoming showers should have minimal but non-zero ZHR (to show they exist)
            assertThat(currentZhr)
                    .as("Upcoming shower should have minimal ZHR")
                    .isLessThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // isFavorable() Tests
    // =========================================================================

    @Nested
    class FavorableTests {
        @Test
        void testIsFavorable_AtPeak_ReturnsTrue() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.isFavorable()).isTrue();
        }

        @Test
        void testIsFavorable_Ended_ReturnsFalse() {
            MeteorShower shower = createEnded();
            assertThat(shower.isFavorable()).isFalse();
        }
    }

    // =========================================================================
    // isAtPeak() Tests
    // =========================================================================

    @Nested
    class PeakTests {
        @Test
        void testIsAtPeak_ActiveAtPeak_ReturnsTrue() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.isAtPeak()).isTrue();
        }

        @Test
        void testIsAtPeak_ActiveNotAtPeak_ReturnsFalse() {
            MeteorShower shower = createActiveNotAtPeak();
            assertThat(shower.isAtPeak()).isFalse();
        }
    }

    // =========================================================================
    // Time Remaining Invariant Tests
    // =========================================================================

    @Nested
    class TimeRemainingTests {
        @Test
        void testGetTimeRemaining_Upcoming_IsPositive() {
            MeteorShower shower = createUpcoming();
            Duration remaining = shower.getTimeRemaining();
            assertThat(remaining.toHours())
                    .as("Upcoming should have positive time remaining")
                    .isPositive();
        }

        @Test
        void testGetTimeRemaining_Active_IsPositive() {
            MeteorShower shower = createActiveAtPeak();
            Duration remaining = shower.getTimeRemaining();
            assertThat(remaining.toHours())
                    .as("Active should have time remaining to end")
                    .isPositive();
        }

        @Test
        void testGetTimeRemaining_Ended_IsNegative() {
            MeteorShower shower = createEnded();
            Duration remaining = shower.getTimeRemaining();
            assertThat(remaining.isNegative())
                    .as("Ended should have negative time remaining")
                    .isTrue();
        }

        @Test
        void testGetTimeRemainingSeconds_EqualsGetTimeRemainingSeconds() {
            MeteorShower shower = createUpcoming();
            assertThat(shower.getTimeRemainingSeconds())
                    .isEqualTo(shower.getTimeRemaining().getSeconds());
        }
    }

    // =========================================================================
    // isEndingSoon() Tests
    // =========================================================================

    @Nested
    class EndingSoonTests {
        @Test
        void testIsEndingSoon_PeakEndingWithin6Hours_ReturnsTrue() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    TEST_SHOWER, TEST_CODE,
                    now.minus(Duration.ofHours(20)),
                    now.plus(Duration.ofHours(5)),  // Peak ends in 5 hours
                    now.minus(Duration.ofDays(1)),
                    now.plus(Duration.ofDays(1)),
                    50, null, null
            );
            assertThat(shower.isEndingSoon()).isTrue();
        }

        @Test
        void testIsEndingSoon_PeakNotEndingSoon_ReturnsFalse() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    TEST_SHOWER, TEST_CODE,
                    now.minus(Duration.ofHours(12)),
                    now.plus(Duration.ofHours(7)),  // Peak ends in 7 hours
                    now.minus(Duration.ofDays(1)),
                    now.plus(Duration.ofDays(1)),
                    50, null, null
            );
            assertThat(shower.isEndingSoon()).isFalse();
        }

        @Test
        void testIsEndingSoon_Upcoming_ReturnsFalse() {
            MeteorShower shower = createUpcoming();
            assertThat(shower.isEndingSoon())
                    .as("Upcoming shower should not be ending soon")
                    .isFalse();
        }

        @Test
        void testIsEndingSoon_Ended_ReturnsFalse() {
            MeteorShower shower = createEnded();
            assertThat(shower.isEndingSoon())
                    .as("Ended shower should not be ending soon")
                    .isFalse();
        }
    }

    // =========================================================================
    // Additional Favorable Tests
    // =========================================================================

    @Nested
    class AdditionalFavorableTests {
        @Test
        void testIsFavorable_ActiveNearPeak_ReturnsTrue() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    TEST_SHOWER, TEST_CODE,
                    now.plus(Duration.ofHours(10)),  // Peak starts in 10 hours
                    now.plus(Duration.ofHours(34)),
                    now.minus(Duration.ofHours(12)), // Visibility started 12 hours ago
                    now.plus(Duration.ofDays(2)),
                    50, null, null
            );
            assertThat(shower.isFavorable())
                    .as("Active shower with peak in 10 hours should be favorable")
                    .isTrue();
        }

        @Test
        void testIsFavorable_UpcomingSoon_ReturnsTrue() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    TEST_SHOWER, TEST_CODE,
                    now.plus(Duration.ofHours(18)),
                    now.plus(Duration.ofHours(42)),
                    now.plus(Duration.ofHours(8)),  // Starts in 8 hours
                    now.plus(Duration.ofDays(2)),
                    50, null, null
            );
            assertThat(shower.isFavorable())
                    .as("Upcoming shower within 12 hours should be favorable")
                    .isTrue();
        }

        @Test
        void testIsFavorable_ActiveFarFromPeak_ReturnsFalse() {
            Instant now = Instant.now();
            MeteorShower shower = new MeteorShower(
                    TEST_SHOWER, TEST_CODE,
                    now.plus(Duration.ofDays(3)),  // Peak starts in 3 days
                    now.plus(Duration.ofDays(4)),
                    now.minus(Duration.ofHours(1)), // Visibility started 1 hour ago
                    now.plus(Duration.ofDays(5)),
                    50, null, null
            );
            assertThat(shower.isFavorable())
                    .as("Active shower with peak in 3 days should not be favorable")
                    .isFalse();
        }
    }

    // =========================================================================
    // Time to Peak Tests
    // =========================================================================

    @Nested
    class TimeToPeakTests {
        @Test
        void testGetTimeToPeak_BeforePeak_IsPositive() {
            MeteorShower shower = createActiveNotAtPeak();
            Duration timeToPeak = shower.getTimeToPeak();
            assertThat(timeToPeak.toHours())
                    .as("Before peak should return positive duration")
                    .isPositive();
        }

        @Test
        void testGetTimeToPeak_AtPeak_IsZero() {
            MeteorShower shower = createActiveAtPeak();
            Duration timeToPeak = shower.getTimeToPeak();
            assertThat(timeToPeak).isEqualTo(Duration.ZERO);
        }

        @Test
        void testGetTimeToPeak_AfterPeak_IsNegative() {
            MeteorShower shower = createEnded();
            Duration timeToPeak = shower.getTimeToPeak();
            assertThat(timeToPeak.isNegative())
                    .as("After peak should return negative duration")
                    .isTrue();
        }

        @Test
        void testGetTimeToPeakSeconds_EqualsGetTimeToPeakSeconds() {
            MeteorShower shower = createActiveNotAtPeak();
            assertThat(shower.getTimeToPeakSeconds())
                    .isEqualTo(shower.getTimeToPeak().getSeconds());
        }
    }

    // =========================================================================
    // Event Interface Tests
    // =========================================================================

    @Nested
    class EventInterfaceTests {
        @Test
        void testGetName_ReturnsCorrectName() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.getName()).isEqualTo(PERSEIDS_2025);
        }

        @Test
        void testGetStartTime_ReturnsVisibilityStart() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.getStartTime()).isEqualTo(shower.visibilityStart());
        }

        @Test
        void testGetEndTime_ReturnsVisibilityEnd() {
            MeteorShower shower = createActiveAtPeak();
            assertThat(shower.getEndTime()).isEqualTo(shower.visibilityEnd());
        }
    }
}
