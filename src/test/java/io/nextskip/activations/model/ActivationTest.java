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
            Activation activation = createActivation(tenMinutesAgo);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should be favorable when spotted within fresh threshold")
                    .isTrue();
        }

        @Test
        void testIsFavorable_ExactlyAtFreshThreshold_ReturnsTrue() {
            // Given: Activation spotted exactly 15 minutes ago
            Instant fifteenMinutesAgo = Instant.now().minus(ACTIVATION_FRESH_THRESHOLD, ChronoUnit.MINUTES);
            Activation activation = createActivation(fifteenMinutesAgo);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should be favorable when spotted exactly at fresh threshold")
                    .isTrue();
        }

        @Test
        void testIsFavorable_BeyondFreshThreshold_ReturnsFalse() {
            // Given: Activation spotted 20 minutes ago
            Instant twentyMinutesAgo = Instant.now().minus(20, ChronoUnit.MINUTES);
            Activation activation = createActivation(twentyMinutesAgo);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should not be favorable when spotted beyond fresh threshold")
                    .isFalse();
        }

        @Test
        void testIsFavorable_NullSpottedAt_ReturnsFalse() {
            // Given: Activation with null spottedAt
            Activation activation = createActivation(null);

            // When/Then
            assertThat(activation.isFavorable())
                    .as("Should not be favorable when spottedAt is null")
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
                Activation activation = createActivation(time);
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
                Activation activation = createActivation(time);
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

            Activation fresh = createActivation(fiveMinAgo);
            Activation aging = createActivation(fifteenMinAgo);
            Activation older = createActivation(thirtyMinAgo);
            Activation stale = createActivation(sixtyMinAgo);

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
            // Boundary: Activations within VERY_FRESH threshold should score maximum
            Instant threeMinutesAgo = Instant.now().minus(3, ChronoUnit.MINUTES);
            Activation activation = createActivation(threeMinutesAgo);

            assertThat(activation.getScore())
                    .as("Very fresh activations should score maximum")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_ExactlyAtVeryFreshThreshold_ReturnsMaxScore() {
            // Boundary: Exactly at the very fresh threshold
            Instant atThreshold = Instant.now().minus(ACTIVATION_VERY_FRESH_THRESHOLD, ChronoUnit.MINUTES);
            Activation activation = createActivation(atThreshold);

            assertThat(activation.getScore())
                    .as("Score at very fresh threshold should be maximum")
                    .isEqualTo(MAX_SCORE);
        }

        @Test
        void testGetScore_VeryStale_ReturnsMinScore() {
            // Boundary: Activations beyond stale threshold should score minimum
            Instant ninetyMinutesAgo = Instant.now().minus(90, ChronoUnit.MINUTES);
            Activation activation = createActivation(ninetyMinutesAgo);

            assertThat(activation.getScore())
                    .as("Very stale activations should score minimum")
                    .isEqualTo(MIN_SCORE);
        }

        @Test
        void testGetScore_NullSpottedAt_ReturnsMinScore() {
            // Edge case: null timestamp
            Activation activation = createActivation(null);

            assertThat(activation.getScore())
                    .as("Null spottedAt should score minimum")
                    .isEqualTo(MIN_SCORE);
        }

        @Test
        void testGetScore_FutureTimestamp_ReturnsMaxScore() {
            // Edge case: future timestamps treated as fresh
            Instant futureTime = Instant.now().plus(10, ChronoUnit.MINUTES);
            Activation activation = createActivation(futureTime);

            assertThat(activation.getScore())
                    .as("Future timestamps should score maximum")
                    .isEqualTo(MAX_SCORE);
        }
    }

    // =========================================================================
    // Interface Compliance Tests
    // =========================================================================

    @Nested
    class InterfaceTests {

        @Test
        void testActivation_ImplementsScoreableInterface() {
            Activation activation = createActivation(Instant.now());

            assertThat(activation)
                    .as("Activation should implement Scoreable interface")
                    .isInstanceOf(io.nextskip.common.api.Scoreable.class);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Helper method to create a test Activation.
     */
    private Activation createActivation(Instant spottedAt) {
        return pota().spottedAt(spottedAt).build();
    }
}
