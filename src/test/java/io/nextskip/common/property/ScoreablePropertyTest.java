package io.nextskip.common.property;

import io.nextskip.activations.model.Activation;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.model.Contest;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.nextskip.test.TestConstants.*;
import static io.nextskip.test.fixtures.ActivationFixtures.pota;
import static io.nextskip.test.fixtures.ContestFixtures.contest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Scoreable implementations using jqwik.
 *
 * <p>Tests verify invariants that must hold across all possible inputs:
 * <ul>
 *   <li>Score bounds: Always [0, 100]</li>
 *   <li>Monotonicity: Score decreases (or stays same) as conditions worsen</li>
 *   <li>Ordering: Better conditions produce higher scores</li>
 * </ul>
 *
 * <p>These property tests complement the unit tests in individual model test classes
 * by providing exhaustive input coverage through random generation.
 */
class ScoreablePropertyTest {

    // ==========================================================================
    // Activation Score Properties
    // ==========================================================================

    @Property(tries = 100)
    void activationScore_AlwaysWithinBounds(
            @ForAll @LongRange(min = -60, max = 120) long minutesAgo) {
        Instant spottedAt = Instant.now().minus(minutesAgo, ChronoUnit.MINUTES);
        Activation activation = createActivation(spottedAt);

        int score = activation.getScore();

        assertThat(score)
                .as("Score at %d minutes should be in [%d, %d]", minutesAgo, MIN_SCORE, MAX_SCORE)
                .isBetween(MIN_SCORE, MAX_SCORE);
    }

    @Property(tries = 50)
    void activationScore_MonotonicallyDecreasesWithAge(
            @ForAll @LongRange(min = 0, max = 60) long olderMinutes,
            @ForAll @LongRange(min = 0, max = 60) long newerMinutes) {

        // Ensure older is actually older
        if (olderMinutes <= newerMinutes) {
            return; // Skip cases where older isn't actually older
        }

        Instant olderTime = Instant.now().minus(olderMinutes, ChronoUnit.MINUTES);
        Instant newerTime = Instant.now().minus(newerMinutes, ChronoUnit.MINUTES);

        Activation older = createActivation(olderTime);
        Activation newer = createActivation(newerTime);

        assertThat(newer.getScore())
                .as("Newer activation (%d min) should score >= older (%d min)",
                        newerMinutes, olderMinutes)
                .isGreaterThanOrEqualTo(older.getScore());
    }

    // ==========================================================================
    // BandCondition Score Properties
    // ==========================================================================

    @Property(tries = 100)
    void bandConditionScore_AlwaysWithinBounds(
            @ForAll("bandConditionRatings") BandConditionRating rating,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double confidence) {

        BandCondition condition = new BandCondition(
                FrequencyBand.BAND_20M, rating, confidence);

        int score = condition.getScore();

        assertThat(score)
                .as("Score for %s at %.2f confidence", rating, confidence)
                .isBetween(MIN_SCORE, MAX_SCORE);
    }

    @Property(tries = 50)
    void bandConditionScore_MaintainsRatingOrder(
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double confidence) {

        int goodScore = new BandCondition(FrequencyBand.BAND_20M,
                BandConditionRating.GOOD, confidence).getScore();
        int fairScore = new BandCondition(FrequencyBand.BAND_20M,
                BandConditionRating.FAIR, confidence).getScore();
        int poorScore = new BandCondition(FrequencyBand.BAND_20M,
                BandConditionRating.POOR, confidence).getScore();
        int unknownScore = new BandCondition(FrequencyBand.BAND_20M,
                BandConditionRating.UNKNOWN, confidence).getScore();

        assertThat(goodScore)
                .as("GOOD >= FAIR at confidence %.2f", confidence)
                .isGreaterThanOrEqualTo(fairScore);
        assertThat(fairScore)
                .as("FAIR >= POOR at confidence %.2f", confidence)
                .isGreaterThanOrEqualTo(poorScore);
        assertThat(poorScore)
                .as("POOR >= UNKNOWN at confidence %.2f", confidence)
                .isGreaterThanOrEqualTo(unknownScore);
    }

    @Property(tries = 50)
    void bandConditionScore_IncreasesWithConfidence(
            @ForAll("bandConditionRatings") BandConditionRating rating,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double lowerConfidence,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double higherConfidence) {

        if (higherConfidence <= lowerConfidence) {
            return; // Skip cases where higher isn't actually higher
        }

        int lowerScore = new BandCondition(FrequencyBand.BAND_20M,
                rating, lowerConfidence).getScore();
        int higherScore = new BandCondition(FrequencyBand.BAND_20M,
                rating, higherConfidence).getScore();

        assertThat(higherScore)
                .as("%s at %.2f confidence should score >= at %.2f",
                        rating, higherConfidence, lowerConfidence)
                .isGreaterThanOrEqualTo(lowerScore);
    }

    // ==========================================================================
    // SolarIndices Score Properties
    // ==========================================================================

    @Property(tries = 100)
    void solarIndicesScore_AlwaysWithinBounds(
            @ForAll @DoubleRange(min = 50.0, max = 300.0) double sfi,
            @ForAll @IntRange(min = 0, max = 9) int kIndex,
            @ForAll @IntRange(min = 0, max = 50) int aIndex) {

        SolarIndices indices = new SolarIndices(
                sfi, aIndex, kIndex, DEFAULT_SUNSPOT_NUMBER,
                Instant.now(), DEFAULT_SOURCE);

        int score = indices.getScore();

        assertThat(score)
                .as("Score for SFI=%.1f, K=%d, A=%d", sfi, kIndex, aIndex)
                .isBetween(MIN_SCORE, MAX_SCORE);
    }

    @Property(tries = 100)
    void solarIndices_FavorableRequiresAllConditions(
            @ForAll @DoubleRange(min = 50.0, max = 200.0) double sfi,
            @ForAll @IntRange(min = 0, max = 9) int kIndex,
            @ForAll @IntRange(min = 0, max = 40) int aIndex) {

        SolarIndices indices = new SolarIndices(
                sfi, aIndex, kIndex, DEFAULT_SUNSPOT_NUMBER,
                Instant.now(), DEFAULT_SOURCE);

        boolean favorable = indices.isFavorable();
        boolean allConditionsMet = sfi > SFI_FAVORABLE_THRESHOLD
                && kIndex < K_INDEX_FAVORABLE_THRESHOLD
                && aIndex < A_INDEX_FAVORABLE_THRESHOLD;

        assertThat(favorable)
                .as("isFavorable (SFI=%.1f, K=%d, A=%d) should equal allConditionsMet",
                        sfi, kIndex, aIndex)
                .isEqualTo(allConditionsMet);
    }

    @Property(tries = 50)
    void solarIndicesScore_HigherSfiMeansHigherOrEqualScore(
            @ForAll @DoubleRange(min = 50.0, max = 200.0) double lowerSfi,
            @ForAll @DoubleRange(min = 50.0, max = 200.0) double higherSfi) {

        if (higherSfi <= lowerSfi) {
            return; // Skip cases where higher isn't actually higher
        }

        SolarIndices lower = new SolarIndices(lowerSfi, DEFAULT_A_INDEX,
                DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER, Instant.now(), DEFAULT_SOURCE);
        SolarIndices higher = new SolarIndices(higherSfi, DEFAULT_A_INDEX,
                DEFAULT_K_INDEX, DEFAULT_SUNSPOT_NUMBER, Instant.now(), DEFAULT_SOURCE);

        assertThat(higher.getScore())
                .as("SFI %.1f should score >= SFI %.1f", higherSfi, lowerSfi)
                .isGreaterThanOrEqualTo(lower.getScore());
    }

    // ==========================================================================
    // Contest Score Properties
    // ==========================================================================

    @Property(tries = 50)
    void contestScore_CloserStartsScoreHigher(
            @ForAll @LongRange(min = 1, max = 100) long earlierHours,
            @ForAll @LongRange(min = 1, max = 100) long laterHours) {

        if (earlierHours >= laterHours) {
            return; // Skip cases where earlier isn't actually earlier
        }

        Instant earlierStart = Instant.now().plus(earlierHours, ChronoUnit.HOURS);
        Instant laterStart = Instant.now().plus(laterHours, ChronoUnit.HOURS);

        Contest earlier = createContest("Earlier", earlierStart,
                earlierStart.plus(24, ChronoUnit.HOURS));
        Contest later = createContest("Later", laterStart,
                laterStart.plus(24, ChronoUnit.HOURS));

        assertThat(earlier.getScore())
                .as("Contest starting in %d hours should score >= contest in %d hours",
                        earlierHours, laterHours)
                .isGreaterThanOrEqualTo(later.getScore());
    }

    @Property(tries = 100)
    void contestScore_AlwaysWithinBounds(
            @ForAll @LongRange(min = -48, max = 120) long hoursFromNow) {

        Instant start = Instant.now().plus(hoursFromNow, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        Contest contest = createContest("Test", start, end);

        int score = contest.getScore();

        assertThat(score)
                .as("Score at %d hours from now", hoursFromNow)
                .isBetween(MIN_SCORE, MAX_SCORE);
    }

    // ==========================================================================
    // Providers (Custom Arbitraries)
    // ==========================================================================

    @Provide
    Arbitrary<BandConditionRating> bandConditionRatings() {
        return Arbitraries.of(BandConditionRating.values());
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    private Activation createActivation(Instant spottedAt) {
        return pota().spottedAt(spottedAt).build();
    }

    private Contest createContest(String name, Instant start, Instant end) {
        return contest().name(name).startTime(start).endTime(end).build();
    }
}
