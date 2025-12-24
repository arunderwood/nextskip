package io.nextskip.activations.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Activation model scoring logic.
 */
class ActivationTest {

    @Test
    void shouldBeFavorable_WhenSpottedWithin15Minutes() {
        // Given: Activation spotted 10 minutes ago
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        Activation activation = createActivation(tenMinutesAgo);

        // When/Then
        assertTrue(activation.isFavorable(), "Should be favorable when spotted within 15 minutes");
    }

    @Test
    void shouldBeFavorable_WhenSpottedExactly15MinutesAgo() {
        // Given: Activation spotted exactly 15 minutes ago
        Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        Activation activation = createActivation(fifteenMinutesAgo);

        // When/Then
        assertTrue(activation.isFavorable(), "Should be favorable when spotted exactly 15 minutes ago");
    }

    @Test
    void shouldNotBeFavorable_WhenSpottedOver15MinutesAgo() {
        // Given: Activation spotted 20 minutes ago
        Instant twentyMinutesAgo = Instant.now().minus(20, ChronoUnit.MINUTES);
        Activation activation = createActivation(twentyMinutesAgo);

        // When/Then
        assertFalse(activation.isFavorable(), "Should not be favorable when spotted over 15 minutes ago");
    }

    @Test
    void shouldNotBeFavorable_WhenSpottedAtIsNull() {
        // Given: Activation with null spottedAt
        Activation activation = createActivation(null);

        // When/Then
        assertFalse(activation.isFavorable(), "Should not be favorable when spottedAt is null");
    }

    @Test
    void shouldScore100_WhenSpottedWithin5Minutes() {
        // Given: Activation spotted 3 minutes ago
        Instant threeMinutesAgo = Instant.now().minus(3, ChronoUnit.MINUTES);
        Activation activation = createActivation(threeMinutesAgo);

        // When/Then
        assertEquals(100, activation.getScore(), "Should score 100 when spotted within 5 minutes");
    }

    @Test
    void shouldScore100_WhenSpottedExactly5MinutesAgo() {
        // Given: Activation spotted exactly 5 minutes ago
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        Activation activation = createActivation(fiveMinutesAgo);

        // When/Then
        assertEquals(100, activation.getScore(), "Should score 100 when spotted exactly 5 minutes ago");
    }

    @Test
    void shouldDecay_LinearlyBetween5And15Minutes() {
        // Given: Activation spotted 10 minutes ago
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        Activation activation = createActivation(tenMinutesAgo);

        // When/Then: 10 minutes = 5 minutes into decay range
        // Score = 100 - ((10 - 5) * 2) = 100 - 10 = 90
        assertEquals(90, activation.getScore(), "Should decay linearly between 5 and 15 minutes");
    }

    @Test
    void shouldScore80_WhenSpotted15MinutesAgo() {
        // Given: Activation spotted 15 minutes ago
        Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        Activation activation = createActivation(fifteenMinutesAgo);

        // When/Then: 15 minutes = boundary condition
        // Score = 100 - ((15 - 5) * 2) = 100 - 20 = 80
        int score = activation.getScore();
        assertTrue(score >= 80 && score <= 82, "Should score ~80 when spotted 15 minutes ago, got " + score);
    }

    @Test
    void shouldDecay_LinearlyBetween15And30Minutes() {
        // Given: Activation spotted 20 minutes ago
        Instant twentyMinutesAgo = Instant.now().minus(20, ChronoUnit.MINUTES);
        Activation activation = createActivation(twentyMinutesAgo);

        // When/Then: 20 minutes = 5 minutes into decay range
        // Score = 80 - ((20 - 15) * 4) = 80 - 20 = 60
        int score = activation.getScore();
        assertTrue(score >= 60 && score <= 62, "Should decay linearly between 15 and 30 minutes, got " + score);
    }

    @Test
    void shouldScore20_WhenSpotted30MinutesAgo() {
        // Given: Activation spotted 30 minutes ago
        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        Activation activation = createActivation(thirtyMinutesAgo);

        // When/Then: 30 minutes = boundary condition
        // Score = 80 - ((30 - 15) * 4) = 80 - 60 = 20
        int score = activation.getScore();
        assertTrue(score >= 20 && score <= 22, "Should score ~20 when spotted 30 minutes ago, got " + score);
    }

    @Test
    void shouldDecay_LinearlyBetween30And60Minutes() {
        // Given: Activation spotted 45 minutes ago
        Instant fortyFiveMinutesAgo = Instant.now().minus(45, ChronoUnit.MINUTES);
        Activation activation = createActivation(fortyFiveMinutesAgo);

        // When/Then: 45 minutes = 15 minutes into decay range
        // Score = max(0, 20 - ((45 - 30) * 0.67)) = max(0, 20 - 10.05) ≈ 10
        int score = activation.getScore();
        assertTrue(score >= 9 && score <= 11, "Should decay linearly between 30 and 60 minutes, got " + score);
    }

    @Test
    void shouldScore0_WhenSpotted60MinutesAgo() {
        // Given: Activation spotted 60 minutes ago
        Instant sixtyMinutesAgo = Instant.now().minus(60, ChronoUnit.MINUTES);
        Activation activation = createActivation(sixtyMinutesAgo);

        // When/Then
        int score = activation.getScore();
        assertTrue(score <= 2, "Should score ~0 when spotted 60 minutes ago, got " + score);
    }

    @Test
    void shouldScore0_WhenSpottedOver60MinutesAgo() {
        // Given: Activation spotted 90 minutes ago
        Instant ninetyMinutesAgo = Instant.now().minus(90, ChronoUnit.MINUTES);
        Activation activation = createActivation(ninetyMinutesAgo);

        // When/Then
        assertEquals(0, activation.getScore(), "Should score 0 when spotted over 60 minutes ago");
    }

    @Test
    void shouldScore0_WhenSpottedAtIsNull() {
        // Given: Activation with null spottedAt
        Activation activation = createActivation(null);

        // When/Then
        assertEquals(0, activation.getScore(), "Should score 0 when spottedAt is null");
    }

    @Test
    void shouldScore100_WhenSpottedInFuture() {
        // Given: Future timestamp (edge case)
        Instant futureTime = Instant.now().plus(10, ChronoUnit.MINUTES);
        Activation activation = createActivation(futureTime);

        // When/Then
        assertEquals(100, activation.getScore(), "Should score 100 for future timestamps");
    }

    @Test
    void shouldImplement_Scoreable() {
        // Given/When
        Activation activation = createActivation(Instant.now());

        // Then: Should implement Scoreable interface
        assertTrue(activation instanceof io.nextskip.common.api.Scoreable,
                "Activation should implement Scoreable interface");
    }

    /**
     * Helper method to create a test Activation.
     */
    private Activation createActivation(Instant spottedAt) {
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
                "12345",
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
