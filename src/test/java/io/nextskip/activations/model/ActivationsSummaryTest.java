package io.nextskip.activations.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActivationsSummary model scoring logic.
 */
class ActivationsSummaryTest {

    @Test
    void shouldBeFavorableWhenTotalCountIs5OrMore() {
        // Given: 5 total activations (3 POTA + 2 SOTA)
        ActivationsSummary summary = createSummary(3, 2, List.of());

        // When/Then
        assertTrue(summary.isFavorable(), "Should be favorable with 5 or more activations");
    }

    @Test
    void shouldBeFavorableWhenTotalCountIsExactly5() {
        // Given: Exactly 5 activations (5 POTA + 0 SOTA)
        ActivationsSummary summary = createSummary(5, 0, List.of());

        // When/Then
        assertTrue(summary.isFavorable(), "Should be favorable with exactly 5 activations");
    }

    @Test
    void shouldNotBeFavorableWhenTotalCountIsLessThan5() {
        // Given: 4 total activations (2 POTA + 2 SOTA)
        ActivationsSummary summary = createSummary(2, 2, List.of());

        // When/Then
        assertFalse(summary.isFavorable(), "Should not be favorable with less than 5 activations");
    }

    @Test
    void shouldNotBeFavorableWhenNoActivations() {
        // Given: No activations
        ActivationsSummary summary = createSummary(0, 0, List.of());

        // When/Then
        assertFalse(summary.isFavorable(), "Should not be favorable with no activations");
    }

    @Test
    void shouldScore0WithNoActivations() {
        // Given: No activations
        ActivationsSummary summary = createSummary(0, 0, List.of());

        // When/Then
        assertEquals(0, summary.getScore(), "Should score 0 with no activations");
    }

    @Test
    void shouldCalculateBaseScoreWithoutRecencyBonus() {
        // Given: 10 activations, all older than 5 minutes
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<Activation> activations = List.of(
                createActivation("1", tenMinutesAgo),
                createActivation("2", tenMinutesAgo)
        );
        ActivationsSummary summary = createSummary(5, 5, activations);

        // When/Then: Base score = 10 * 3 = 30, no recency bonus
        assertEquals(30, summary.getScore(), "Should calculate base score without recency bonus");
    }

    @Test
    void shouldAddRecencyBonusForRecentActivation() {
        // Given: 10 activations, one spotted 3 minutes ago
        Instant threeMinutesAgo = Instant.now().minus(3, ChronoUnit.MINUTES);
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<Activation> activations = List.of(
                createActivation("1", threeMinutesAgo),
                createActivation("2", tenMinutesAgo)
        );
        ActivationsSummary summary = createSummary(5, 5, activations);

        // When/Then: Base score = 10 * 3 = 30, recency bonus = 10, total = 40
        assertEquals(40, summary.getScore(), "Should add recency bonus for activation within 5 minutes");
    }

    @Test
    void shouldAddRecencyBonusWhenActivationExactly5MinutesOld() {
        // Given: Activation spotted exactly 5 minutes ago
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<Activation> activations = List.of(
                createActivation("1", fiveMinutesAgo)
        );
        ActivationsSummary summary = createSummary(10, 0, activations);

        // When/Then: Base score = 10 * 3 = 30, recency bonus = 10, total = 40
        assertEquals(40, summary.getScore(), "Should add recency bonus when activation is exactly 5 minutes old");
    }

    @Test
    void shouldNotAddRecencyBonusWhenActivationOver5MinutesOld() {
        // Given: Activation spotted 6 minutes ago
        Instant sixMinutesAgo = Instant.now().minus(6, ChronoUnit.MINUTES);
        List<Activation> activations = List.of(
                createActivation("1", sixMinutesAgo)
        );
        ActivationsSummary summary = createSummary(10, 0, activations);

        // When/Then: Base score = 10 * 3 = 30, no recency bonus
        assertEquals(30, summary.getScore(), "Should not add recency bonus when activation is over 5 minutes old");
    }

    @Test
    void shouldCapScoreAt100() {
        // Given: 40 activations (would score 120 before capping)
        ActivationsSummary summary = createSummary(20, 20, List.of());

        // When/Then: Base score = 40 * 3 = 120, capped at 100
        assertEquals(100, summary.getScore(), "Should cap score at 100");
    }

    @Test
    void shouldCapScoreAt100WithRecencyBonus() {
        // Given: 34 activations with recent activity
        Instant now = Instant.now();
        List<Activation> activations = List.of(createActivation("1", now));
        ActivationsSummary summary = createSummary(17, 17, activations);

        // When/Then: Base score = 34 * 3 = 102, + 10 bonus = 112, capped at 100
        assertEquals(100, summary.getScore(), "Should cap score at 100 even with recency bonus");
    }

    @Test
    void shouldScoreCorrectlyFor3Activations() {
        // Given: 3 activations (below favorable threshold)
        ActivationsSummary summary = createSummary(2, 1, List.of());

        // When/Then: Base score = 3 * 3 = 9
        assertEquals(9, summary.getScore(), "Should score 9 for 3 activations");
    }

    @Test
    void shouldScoreCorrectlyFor5ActivationsWithRecentActivity() {
        // Given: 5 activations with recent activity
        Instant now = Instant.now();
        List<Activation> activations = List.of(createActivation("1", now));
        ActivationsSummary summary = createSummary(3, 2, activations);

        // When/Then: Base score = 5 * 3 = 15, + 10 bonus = 25
        assertEquals(25, summary.getScore(), "Should score 25 for 5 recent activations");
    }

    @Test
    void shouldScoreCorrectlyFor20ActivationsWithRecentActivity() {
        // Given: 20 activations with recent activity
        Instant now = Instant.now();
        List<Activation> activations = List.of(createActivation("1", now));
        ActivationsSummary summary = createSummary(10, 10, activations);

        // When/Then: Base score = 20 * 3 = 60, + 10 bonus = 70 (hot threshold)
        assertEquals(70, summary.getScore(), "Should score 70 for 20 recent activations");
    }

    @Test
    void shouldHandleNullActivationsList() {
        // Given: Null activations list
        ActivationsSummary summary = new ActivationsSummary(null, 10, 5, Instant.now());

        // When/Then: Base score = 15 * 3 = 45, no recency bonus (null list)
        assertEquals(45, summary.getScore(), "Should handle null activations list gracefully");
    }

    @Test
    void shouldHandleEmptyActivationsList() {
        // Given: Empty activations list but positive counts (edge case)
        ActivationsSummary summary = createSummary(5, 5, List.of());

        // When/Then: Base score = 10 * 3 = 30, no recency bonus
        assertEquals(30, summary.getScore(), "Should handle empty activations list");
    }

    @Test
    void shouldHandleActivationsWithNullSpottedAt() {
        // Given: Activations with null spottedAt timestamps
        List<Activation> activations = List.of(
                createActivation("1", null),
                createActivation("2", null)
        );
        ActivationsSummary summary = createSummary(5, 5, activations);

        // When/Then: Base score = 10 * 3 = 30, no recency bonus (null timestamps)
        assertEquals(30, summary.getScore(), "Should handle activations with null spottedAt");
    }

    @Test
    void shouldImplementScoreable() {
        // Given/When
        ActivationsSummary summary = createSummary(5, 5, List.of());

        // Then: Should implement Scoreable interface
        assertTrue(summary instanceof io.nextskip.common.api.Scoreable,
                "ActivationsSummary should implement Scoreable interface");
    }

    /**
     * Helper method to create a test ActivationsSummary.
     */
    private ActivationsSummary createSummary(int potaCount, int sotaCount, List<Activation> activations) {
        return new ActivationsSummary(
                activations,
                potaCount,
                sotaCount,
                Instant.now()
        );
    }

    /**
     * Helper method to create a test Activation.
     */
    private Activation createActivation(String id, Instant spottedAt) {
        io.nextskip.activations.model.Park park = new io.nextskip.activations.model.Park(
                "US-0001",
                "Test Park",
                "CO",
                "US",
                "FN42",
                42.5,
                -71.3
        );

        return new Activation(
                id,
                "W1ABC",
                ActivationType.POTA,
                14250.0,
                "SSB",
                spottedAt,
                10,
                "Test Source",
                park
        );
    }
}
