package io.nextskip.activations.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static io.nextskip.test.TestConstants.*;
import static io.nextskip.test.fixtures.ActivationFixtures.pota;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Activation model scoring logic.
 *
 * <p>Tests use invariant-based assertions to verify scoring contracts
 * without coupling to specific algorithm coefficients.
 *
 * <p>Scoring is based on {@code spottedAt} (when someone reported hearing the station),
 * not {@code lastSeenAt} (when our API client observed the spot). This provides
 * true freshness indication of when the activator was actually heard.
 */
class ActivationTest {

    // =========================================================================
    // isFavorable() Tests
    // =========================================================================

    @Nested
    class IsFavorableTests {

        @Test
        void testIsFavorable_WithinFreshThreshold_ReturnsTrue() {
            // Given: Activation spotted 10 minutes ago (within 15-minute fresh threshold)
            Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(tenMinutesAgo);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should be favorable when spotted within fresh threshold")
                    .isTrue();
        }

        @Test
        void testIsFavorable_ExactlyAtFreshThreshold_ReturnsTrue() {
            // Given: Activation spotted exactly 15 minutes ago
            Instant fifteenMinutesAgo = Instant.now().minus(ACTIVATION_FRESH_THRESHOLD, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(fifteenMinutesAgo);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should be favorable when spotted exactly at fresh threshold")
                    .isTrue();
        }

        @Test
        void testIsFavorable_BeyondFreshThreshold_ReturnsFalse() {
            // Given: Activation spotted 20 minutes ago
            Instant twentyMinutesAgo = Instant.now().minus(20, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(twentyMinutesAgo);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should not be favorable when spotted beyond fresh threshold")
                    .isFalse();
        }

        @Test
        void testIsFavorable_NullSpottedAt_ReturnsFalse() {
            // Given: Activation with null spottedAt
            Activation activation = createActivationWithSpottedAt(null);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should not be favorable when spottedAt is null")
                    .isFalse();
        }
    }

    // =========================================================================
    // isFavorable(Instant asOf) Deterministic Tests
    // =========================================================================

    @Nested
    class IsFavorableDeterministicTests {

        @Test
        void testIsFavorable_WithExplicitAsOf_IsDeterministic() {
            // Given: Fixed reference time for deterministic testing
            Instant now = Instant.parse(REFERENCE_TIME_STRING);
            Instant spotted = now.minus(10, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(spotted);

            // When/Then: Using explicit asOf parameter
            assertThat(activation.isFavorable(now))
                    .as("Should be favorable with explicit reference time")
                    .isTrue();
        }

        @Test
        void testIsFavorable_BeyondThresholdWithExplicitAsOf_ReturnsFalse() {
            // Given: Fixed reference time for deterministic testing
            Instant now = Instant.parse(REFERENCE_TIME_STRING);
            Instant spotted = now.minus(20, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(spotted);

            // When/Then
            assertThat(activation.isFavorable(now))
                    .as("Should not be favorable when spotted 20 minutes before reference time")
                    .isFalse();
        }
    }

    // =========================================================================
    // getScore() Invariant Tests
    // =========================================================================

    @Nested
    class ScoreInvariantTests {

        @Test
        void testGetScore_AlwaysWithinBounds() {
            // Test score bounds at various ages
            long[] minutesAgoValues = {0, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120};

            for (long minutesAgo : minutesAgoValues) {
                Instant time = Instant.now().minus(minutesAgo, ChronoUnit.MINUTES);
                Activation activation = createActivationWithSpottedAt(time);
                int score = activation.getScore();

                assertThat(score)
                        .as("Score at %d minutes should be within [%d, %d]", minutesAgo, MIN_SCORE, MAX_SCORE)
                        .isBetween(MIN_SCORE, MAX_SCORE);
            }
        }

        @Test
        void testGetScore_MonotonicallyDecreasesWithAge() {
            // Invariant: score(t1) >= score(t2) when t1 < t2 (newer >= older)
            List<Integer> scores = new ArrayList<>();

            for (long minutesAgo = 0; minutesAgo <= 90; minutesAgo += 5) {
                Instant time = Instant.now().minus(minutesAgo, ChronoUnit.MINUTES);
                Activation activation = createActivationWithSpottedAt(time);
                scores.add(activation.getScore());
            }

            // Verify monotonic non-increasing
            for (int i = 1; i < scores.size(); i++) {
                assertThat(scores.get(i))
                        .as("Score should decrease (or stay same) as age increases: index %d", i)
                        .isLessThanOrEqualTo(scores.get(i - 1));
            }
        }

        @Test
        void testGetScore_FresherActivationsScoreHigherOrEqual() {
            // Invariant: fresher always >= older
            Instant fiveMinAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            Instant fifteenMinAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
            Instant thirtyMinAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
            Instant sixtyMinAgo = Instant.now().minus(60, ChronoUnit.MINUTES);

            Activation fresh = createActivationWithSpottedAt(fiveMinAgo);
            Activation aging = createActivationWithSpottedAt(fifteenMinAgo);
            Activation older = createActivationWithSpottedAt(thirtyMinAgo);
            Activation stale = createActivationWithSpottedAt(sixtyMinAgo);

            assertThat(fresh.getScore())
                    .as("Fresh >= Aging")
                    .isGreaterThanOrEqualTo(aging.getScore());
            assertThat(aging.getScore())
                    .as("Aging >= Older")
                    .isGreaterThanOrEqualTo(older.getScore());
            assertThat(older.getScore())
                    .as("Older >= Stale")
                    .isGreaterThanOrEqualTo(stale.getScore());
        }
    }

    // =========================================================================
    // getScore() Boundary Tests (true boundaries only)
    // =========================================================================

    @Nested
    class ScoreBoundaryTests {

        @Test
        void testGetScore_VeryFresh_ReturnsMaxScore() {
            // Boundary: Activations spotted within VERY_FRESH threshold should score maximum
            Instant threeMinutesAgo = Instant.now().minus(3, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(threeMinutesAgo);

            assertThat(activation.getScore())
                    .as("Very fresh activations should score maximum")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_ExactlyAtVeryFreshThreshold_ReturnsMaxScore() {
            // Boundary: Exactly at the very fresh threshold
            Instant atThreshold = Instant.now().minus(ACTIVATION_VERY_FRESH_THRESHOLD, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(atThreshold);

            assertThat(activation.getScore())
                    .as("Score at very fresh threshold should be maximum")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_VeryStale_ReturnsMinScore() {
            // Boundary: Activations spotted beyond stale threshold should score minimum
            Instant ninetyMinutesAgo = Instant.now().minus(90, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(ninetyMinutesAgo);

            assertThat(activation.getScore())
                    .as("Very stale activations should score minimum")
                    .isEqualTo(MIN_SCORE);
        }

        @Test
        void testGetScore_NullSpottedAt_ReturnsMinScore() {
            // Edge case: null spottedAt
            Activation activation = createActivationWithSpottedAt(null);

            assertThat(activation.getScore())
                    .as("Null spottedAt should score minimum")
                    .isEqualTo(MIN_SCORE);
        }

        @Test
        void testGetScore_FutureTimestamp_ReturnsMaxScore() {
            // Edge case: future timestamps treated as fresh
            Instant futureTime = Instant.now().plus(10, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(futureTime);

            assertThat(activation.getScore())
                    .as("Future timestamps should score maximum")
                    .isEqualTo(MAX_SCORE);
        }
    }

    // =========================================================================
    // getScore(Instant asOf) Deterministic Tests
    // =========================================================================

    @Nested
    class ScoreDeterministicTests {

        @Test
        void testGetScore_WithExplicitAsOf_IsDeterministic() {
            // Given: Fixed reference time for deterministic testing
            Instant now = Instant.parse(REFERENCE_TIME_STRING);
            Instant spotted = now.minus(3, ChronoUnit.MINUTES);
            Activation activation = createActivationWithSpottedAt(spotted);

            // When/Then: Using explicit asOf parameter
            assertThat(activation.getScore(now))
                    .as("Very fresh activation (3 min) should score maximum")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_MonotonicWithExplicitAsOf() {
            // Given: Fixed reference time
            Instant now = Instant.parse(REFERENCE_TIME_STRING);

            // Create activations with different spotted times
            Activation fresh = createActivationWithSpottedAt(now.minus(5, ChronoUnit.MINUTES));
            Activation aging = createActivationWithSpottedAt(now.minus(20, ChronoUnit.MINUTES));
            Activation stale = createActivationWithSpottedAt(now.minus(45, ChronoUnit.MINUTES));

            // When/Then: Verify monotonic decrease using explicit asOf
            assertThat(fresh.getScore(now))
                    .as("Fresh >= Aging with explicit asOf")
                    .isGreaterThanOrEqualTo(aging.getScore(now));
            assertThat(aging.getScore(now))
                    .as("Aging >= Stale with explicit asOf")
                    .isGreaterThanOrEqualTo(stale.getScore(now));
        }
    }

    // =========================================================================
    // SpottedAt Behavior Tests
    // =========================================================================

    @Nested
    class SpottedAtBehaviorTests {

        @Test
        void testGetScore_UsesSpottedAt_NotLastSeenAt() {
            // Given: Activation with old spottedAt but recent lastSeenAt
            Instant now = Instant.parse(REFERENCE_TIME_STRING);
            Instant originalSpot = now.minus(2, ChronoUnit.HOURS);  // Old spot
            Instant recentlySeen = now.minus(5, ChronoUnit.MINUTES);  // Recent observation

            Activation activation = pota()
                    .spottedAt(originalSpot)
                    .lastSeenAt(recentlySeen)
                    .build();

            // When/Then: Score should be based on spottedAt (2 hours), not lastSeenAt (5 min)
            // Old spot = low score, regardless of when we scraped it
            assertThat(activation.getScore(now))
                    .as("Score should be based on spottedAt (2 hours old), not lastSeenAt")
                    .isEqualTo(MIN_SCORE);
        }

        @Test
        void testGetScore_RecentSpottedAt_OldLastSeenAt_ReturnsHighScore() {
            // Given: Activation with recent spottedAt but old lastSeenAt
            Instant now = Instant.parse(REFERENCE_TIME_STRING);
            Instant recentSpot = now.minus(3, ChronoUnit.MINUTES);  // Fresh spot
            Instant oldLastSeen = now.minus(2, ChronoUnit.HOURS);  // Old observation

            Activation activation = pota()
                    .spottedAt(recentSpot)
                    .lastSeenAt(oldLastSeen)
                    .build();

            // When/Then: Score should be based on spottedAt (3 min), not lastSeenAt (2 hours)
            // Recent spot = high score, regardless of when we scraped it
            assertThat(activation.getScore(now))
                    .as("Score should be based on spottedAt (3 min old), not lastSeenAt")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testIsFavorable_UsesSpottedAt_NotLastSeenAt() {
            // Given: Activation with old spottedAt but recent lastSeenAt
            Instant now = Instant.parse(REFERENCE_TIME_STRING);
            Instant originalSpot = now.minus(2, ChronoUnit.HOURS);
            Instant recentlySeen = now.minus(10, ChronoUnit.MINUTES);

            Activation activation = pota()
                    .spottedAt(originalSpot)
                    .lastSeenAt(recentlySeen)
                    .build();

            // When/Then: Favorable should be based on spottedAt (2 hours), not lastSeenAt (10 min)
            assertThat(activation.isFavorable(now))
                    .as("Favorable should be based on spottedAt (2 hours old), not lastSeenAt")
                    .isFalse();
        }

        @Test
        void testGetScore_NullSpottedAt_ReturnsZero() {
            // Given: Activation with null spottedAt
            Activation activation = pota()
                    .spottedAt(null)
                    .build();

            // When/Then: Should return 0 score (defensive - shouldn't happen in production)
            assertThat(activation.getScore())
                    .as("Null spottedAt should return zero score")
                    .isEqualTo(MIN_SCORE);
        }
    }

    // =========================================================================
    // Interface Compliance Tests
    // =========================================================================

    @Nested
    class InterfaceTests {

        @Test
        void testActivation_ImplementsScoreableInterface() {
            Activation activation = createActivationWithSpottedAt(Instant.now());

            assertThat(activation)
                    .as("Activation should implement Scoreable interface")
                    .isInstanceOf(io.nextskip.common.api.Scoreable.class);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Helper method to create a test Activation with specified spottedAt.
     *
     * <p>Scoring is based on spottedAt, so this is the primary helper for score tests.
     */
    private Activation createActivationWithSpottedAt(Instant spottedAt) {
        return pota().spottedAt(spottedAt).build();
    }
}
