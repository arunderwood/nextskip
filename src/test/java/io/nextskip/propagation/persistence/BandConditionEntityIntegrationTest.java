package io.nextskip.propagation.persistence;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.test.AbstractPersistenceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for BandConditionEntity and repository operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Entity-to-domain and domain-to-entity conversions</li>
 *   <li>Enum storage as VARCHAR with CHECK constraints</li>
 *   <li>Repository query methods</li>
 *   <li>Constraint enforcement</li>
 * </ul>
 */
class BandConditionEntityIntegrationTest extends AbstractPersistenceTest {

    @Autowired
    private BandConditionRepository repository;

    @Override
    protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
        return List.of(repository);
    }

    @Test
    void testFromDomain_PreservesAllFields() {
        // Given: A domain model
        var domain = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.85, "DX open");

        // When: Convert to entity with explicit timestamp
        var now = Instant.now();
        var entity = BandConditionEntity.fromDomain(domain, now);

        // Then: All fields should be preserved
        assertEquals(FrequencyBand.BAND_20M, entity.getBand());
        assertEquals(BandConditionRating.GOOD, entity.getRating());
        assertEquals(0.85, entity.getConfidence());
        assertEquals("DX open", entity.getNotes());
        assertEquals(now, entity.getRecordedAt());
    }

    @Test
    void testFromDomain_WithoutTimestamp_UsesCurrentTime() {
        // Given: A domain model
        var domain = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD);
        var before = Instant.now();

        // When: Convert to entity without explicit timestamp
        var entity = BandConditionEntity.fromDomain(domain);
        var after = Instant.now();

        // Then: Timestamp should be between before and after
        assertNotNull(entity.getRecordedAt());
        assertTrue(entity.getRecordedAt().compareTo(before) >= 0);
        assertTrue(entity.getRecordedAt().compareTo(after) <= 0);
    }

    @Test
    void testToDomain_PreservesAllFields() {
        // Given: An entity
        var now = Instant.now();
        var entity = new BandConditionEntity(
                FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.7, "Moderate activity", now);

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: All domain fields should be preserved
        assertEquals(FrequencyBand.BAND_40M, domain.band());
        assertEquals(BandConditionRating.FAIR, domain.rating());
        assertEquals(0.7, domain.confidence());
        assertEquals("Moderate activity", domain.notes());
    }

    @Test
    void testToDomain_HandlesNullNotes() {
        // Given: An entity with null notes
        var entity = new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.9, null, Instant.now());

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: Notes should be null
        assertNull(domain.notes());
    }

    @Test
    void testRoundTrip_DomainToEntityToDomain_PreservesEquality() {
        // Given: A domain model
        var original = new BandCondition(FrequencyBand.BAND_15M, BandConditionRating.POOR, 0.4, "QSB");

        // When: Convert to entity and back to domain
        var roundTripped = BandConditionEntity.fromDomain(original).toDomain();

        // Then: Should be equal to original
        assertEquals(original, roundTripped);
    }

    @Test
    void testSaveAndRetrieve_Success() {
        // Given: A domain model converted to entity
        var domain = new BandCondition(FrequencyBand.BAND_10M, BandConditionRating.GOOD, 0.95, "Wide open");
        var entity = BandConditionEntity.fromDomain(domain);

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID
        assertNotNull(saved.getId());

        // And: Should be retrievable with correct enum values
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(FrequencyBand.BAND_10M, found.get().getBand());
        assertEquals(BandConditionRating.GOOD, found.get().getRating());
    }

    @Test
    void testFindTopByBandOrderByRecordedAtDesc_ReturnsLatestForBand() {
        // Given: Multiple entries for same band at different times
        var older = Instant.now().minus(1, ChronoUnit.HOURS);
        var newer = Instant.now();

        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.FAIR, 0.6, null, older));
        var latest = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.9, null, newer));

        // When: Find most recent for band
        var result = repository.findTopByBandOrderByRecordedAtDesc(FrequencyBand.BAND_20M);

        // Then: Should return the latest entry
        assertTrue(result.isPresent());
        assertEquals(latest.getId(), result.get().getId());
        assertEquals(BandConditionRating.GOOD, result.get().getRating());
    }

    @Test
    void testFindTopByBandOrderByRecordedAtDesc_FiltersByBand() {
        // Given: Entries for different bands
        var now = Instant.now();
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.9, null, now));
        var band40 = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.7, null, now));

        // When: Find most recent for 40m band
        var result = repository.findTopByBandOrderByRecordedAtDesc(FrequencyBand.BAND_40M);

        // Then: Should return only 40m entry
        assertTrue(result.isPresent());
        assertEquals(band40.getId(), result.get().getId());
    }

    @Test
    void testFindByBandAndRecordedAtAfterOrderByRecordedAtDesc_ReturnsRecentForBand() {
        // Given: Multiple entries for same band at different times
        var now = Instant.now();
        var twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        var oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_6M, BandConditionRating.POOR, 0.3, null, twoHoursAgo));
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_6M, BandConditionRating.FAIR, 0.6, null, oneHourAgo));
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_6M, BandConditionRating.GOOD, 0.9, null, now));

        // When: Find entries after 90 minutes ago
        var cutoff = now.minus(90, ChronoUnit.MINUTES);
        var result = repository.findByBandAndRecordedAtAfterOrderByRecordedAtDesc(
                FrequencyBand.BAND_6M, cutoff);

        // Then: Should return only recent entries, ordered by timestamp desc
        assertEquals(2, result.size());
        assertEquals(BandConditionRating.GOOD, result.get(0).getRating()); // Most recent first
        assertEquals(BandConditionRating.FAIR, result.get(1).getRating());
    }

    @Test
    void testSaveWithNullBand_ThrowsException() {
        // Given: An entity with null band (required field)
        var entity = new BandConditionEntity(
                null, BandConditionRating.GOOD, 0.9, null, Instant.now());

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testSaveWithNullRating_ThrowsException() {
        // Given: An entity with null rating (required field)
        var entity = new BandConditionEntity(
                FrequencyBand.BAND_20M, null, 0.9, null, Instant.now());

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testConvertedDomainModel_CanCalculateScore() {
        // Given: A persisted entity with GOOD rating
        var entity = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.85, null, Instant.now()));

        // When: Convert to domain and calculate score
        var domain = entity.toDomain();
        var score = domain.getScore();

        // Then: Score should be calculated correctly (100 * 0.85 = 85)
        assertEquals(85, score);
    }

    @Test
    void testConvertedDomainModel_CanDetermineIfFavorable() {
        // Given: A GOOD condition with high confidence
        var goodEntity = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.8, null, Instant.now()));

        // And: A FAIR condition
        var fairEntity = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_40M, BandConditionRating.FAIR, 0.9, null, Instant.now()));

        // When: Convert to domain
        var good = goodEntity.toDomain();
        var fair = fairEntity.toDomain();

        // Then: GOOD with high confidence should be favorable
        assertTrue(good.isFavorable());
        // And: FAIR should not be favorable regardless of confidence
        assertFalse(fair.isFavorable());
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Testing all enum values
    void testAllFrequencyBands_CanBePersisted() {
        // Given/When: Save an entity for each frequency band
        var now = Instant.now();
        for (FrequencyBand band : FrequencyBand.values()) {
            var entity = new BandConditionEntity(
                    band, BandConditionRating.GOOD, 0.9, null, now);
            var saved = repository.save(entity);

            // Then: Should be retrievable with correct band
            var found = repository.findById(saved.getId());
            assertTrue(found.isPresent(), "Should find entity for band: " + band);
            assertEquals(band, found.get().getBand());
        }
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Testing all enum values
    void testAllBandConditionRatings_CanBePersisted() {
        // Given/When: Save an entity for each rating
        var now = Instant.now();
        for (BandConditionRating rating : BandConditionRating.values()) {
            var entity = new BandConditionEntity(
                    FrequencyBand.BAND_20M, rating, 0.9, null, now);
            var saved = repository.save(entity);

            // Then: Should be retrievable with correct rating
            var found = repository.findById(saved.getId());
            assertTrue(found.isPresent(), "Should find entity for rating: " + rating);
            assertEquals(rating, found.get().getRating());
        }
    }

    // === Native Query Tests ===

    @Test
    void testFindLatestPerBandSince_ReturnsOnePerBand() {
        // Given: Multiple entries per band simulating multiple refresh cycles
        // Use BAND_12M and BAND_17M to avoid test isolation issues
        var now = Instant.now();
        var thirtyMinutesAgo = now.minus(30, ChronoUnit.MINUTES);
        var fifteenMinutesAgo = now.minus(15, ChronoUnit.MINUTES);

        // First refresh cycle (oldest)
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_12M, BandConditionRating.POOR, 0.4, null, thirtyMinutesAgo));
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_17M, BandConditionRating.FAIR, 0.5, null, thirtyMinutesAgo));

        // Second refresh cycle
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_12M, BandConditionRating.FAIR, 0.6, null, fifteenMinutesAgo));
        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_17M, BandConditionRating.GOOD, 0.7, null, fifteenMinutesAgo));

        // Third refresh cycle (newest)
        var latest12m = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_12M, BandConditionRating.GOOD, 0.9, null, now));
        var latest17m = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_17M, BandConditionRating.GOOD, 0.95, null, now));

        // When: Query for latest per band (6 records inserted, should get 2 back)
        var cutoff = now.minus(1, ChronoUnit.HOURS);
        var result = repository.findLatestPerBandSince(cutoff);

        // Then: Should return exactly one record per unique band
        var band12mResults = result.stream()
                .filter(e -> e.getBand() == FrequencyBand.BAND_12M)
                .toList();
        var band17mResults = result.stream()
                .filter(e -> e.getBand() == FrequencyBand.BAND_17M)
                .toList();

        assertEquals(1, band12mResults.size(), "Should have exactly one 12m record");
        assertEquals(1, band17mResults.size(), "Should have exactly one 17m record");

        // And: Each should be the most recent record for that band
        assertEquals(latest12m.getId(), band12mResults.get(0).getId());
        assertEquals(latest17m.getId(), band17mResults.get(0).getId());
        assertEquals(BandConditionRating.GOOD, band12mResults.get(0).getRating());
        assertEquals(BandConditionRating.GOOD, band17mResults.get(0).getRating());
    }

    @Test
    void testFindLatestPerBandSince_ExcludesRecordsBeforeCutoff() {
        // Given: Records both before and after cutoff for BAND_30M
        var now = Instant.now();
        var twoHoursAgo = now.minus(2, ChronoUnit.HOURS);

        repository.save(new BandConditionEntity(
                FrequencyBand.BAND_30M, BandConditionRating.GOOD, 0.9, null, twoHoursAgo));
        var recent = repository.save(new BandConditionEntity(
                FrequencyBand.BAND_30M, BandConditionRating.FAIR, 0.6, null, now));

        // When: Query with 1 hour cutoff
        var cutoff = now.minus(1, ChronoUnit.HOURS);
        var result = repository.findLatestPerBandSince(cutoff);

        // Then: Should only return the recent record
        var band30mResults = result.stream()
                .filter(e -> e.getBand() == FrequencyBand.BAND_30M)
                .toList();

        assertEquals(1, band30mResults.size());
        assertEquals(recent.getId(), band30mResults.get(0).getId());
    }

    // === Setter Coverage Tests ===

    @Test
    void testSetters_AllFields_UpdatesEntity() {
        // Given: An entity created via constructor
        var now = Instant.now();
        var entity = new BandConditionEntity(
                FrequencyBand.BAND_20M, BandConditionRating.GOOD, 0.85, "Initial notes", now);
        var saved = repository.save(entity);

        // When: Update all fields via setters
        var newTime = Instant.now().plus(1, ChronoUnit.HOURS);
        saved.setBand(FrequencyBand.BAND_40M);
        saved.setRating(BandConditionRating.FAIR);
        saved.setConfidence(0.65);
        saved.setNotes("Updated notes");
        saved.setRecordedAt(newTime);

        // Then: All getters should return updated values
        assertEquals(FrequencyBand.BAND_40M, saved.getBand());
        assertEquals(BandConditionRating.FAIR, saved.getRating());
        assertEquals(0.65, saved.getConfidence());
        assertEquals("Updated notes", saved.getNotes());
        assertEquals(newTime, saved.getRecordedAt());
    }
}
