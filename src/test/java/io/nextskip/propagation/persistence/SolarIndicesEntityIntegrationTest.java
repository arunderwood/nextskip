package io.nextskip.propagation.persistence;

import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import io.nextskip.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for SolarIndicesEntity and repository operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Entity-to-domain and domain-to-entity conversions</li>
 *   <li>Repository CRUD operations</li>
 *   <li>Custom query methods</li>
 *   <li>Constraint enforcement</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class SolarIndicesEntityIntegrationTest extends AbstractIntegrationTest {

    // Use unique test source names to avoid conflicts with scheduler data
    private static final String SOURCE_NOAA = "TEST_NOAA";
    private static final String SOURCE_HAMQSL = "TEST_HAMQSL";

    @Autowired
    private SolarIndicesRepository repository;

    @BeforeEach
    void cleanUp() {
        // Clean slate for tests using unfiltered queries (e.g., findByTimestampAfter)
        repository.deleteAll();
    }

    @Test
    void testFromDomain_PreservesAllFields() {
        // Given: A domain model
        var now = Instant.now();
        var domain = new SolarIndices(150.5, 10, 3, 100, now, SOURCE_NOAA);

        // When: Convert to entity
        var entity = SolarIndicesEntity.fromDomain(domain);

        // Then: All fields should be preserved
        assertEquals(150.5, entity.getSolarFluxIndex());
        assertEquals(10, entity.getAIndex());
        assertEquals(3, entity.getKIndex());
        assertEquals(100, entity.getSunspotNumber());
        assertEquals(now, entity.getTimestamp());
        assertEquals(SOURCE_NOAA, entity.getSource());
    }

    @Test
    void testToDomain_PreservesAllFields() {
        // Given: An entity
        var now = Instant.now();
        var entity = new SolarIndicesEntity(150.5, 10, 3, 100, now, SOURCE_NOAA);

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: All fields should be preserved
        assertEquals(150.5, domain.solarFluxIndex());
        assertEquals(10, domain.aIndex());
        assertEquals(3, domain.kIndex());
        assertEquals(100, domain.sunspotNumber());
        assertEquals(now, domain.timestamp());
        assertEquals(SOURCE_NOAA, domain.source());
    }

    @Test
    void testRoundTrip_DomainToEntityToDomain_PreservesEquality() {
        // Given: A domain model
        var original = new SolarIndices(150.5, 10, 3, 100, Instant.now(), SOURCE_NOAA);

        // When: Convert to entity and back to domain
        var roundTripped = SolarIndicesEntity.fromDomain(original).toDomain();

        // Then: Should be equal to original
        assertEquals(original, roundTripped);
    }

    @Test
    void testSaveAndRetrieve_Success() {
        // Given: A domain model converted to entity
        var domain = createSampleDomain();
        var entity = SolarIndicesEntity.fromDomain(domain);

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID
        assertNotNull(saved.getId());

        // And: Should be retrievable
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(domain.solarFluxIndex(), found.get().getSolarFluxIndex());
    }

    @Test
    void testFindTopBySourceOrderByTimestampDesc_ReturnsLatestForSource() {
        // Given: Multiple entries for same source at different times
        var older = Instant.now().minus(1, ChronoUnit.HOURS);
        var newer = Instant.now();

        repository.save(new SolarIndicesEntity(100.0, 5, 2, 50, older, SOURCE_NOAA));
        var latest = repository.save(new SolarIndicesEntity(150.0, 10, 3, 100, newer, SOURCE_NOAA));

        // When: Find most recent for source
        var result = repository.findTopBySourceOrderByTimestampDesc(SOURCE_NOAA);

        // Then: Should return the latest entry
        assertTrue(result.isPresent());
        assertEquals(latest.getId(), result.get().getId());
        assertEquals(150.0, result.get().getSolarFluxIndex());
    }

    @Test
    void testFindTopBySourceOrderByTimestampDesc_FiltersbySource() {
        // Given: Entries from different sources
        var now = Instant.now();
        repository.save(new SolarIndicesEntity(100.0, 5, 2, 50, now, SOURCE_NOAA));
        var hamqsl = repository.save(new SolarIndicesEntity(150.0, 10, 3, 100, now, SOURCE_HAMQSL));

        // When: Find most recent for HamQSL source
        var result = repository.findTopBySourceOrderByTimestampDesc(SOURCE_HAMQSL);

        // Then: Should return only HamQSL entry
        assertTrue(result.isPresent());
        assertEquals(hamqsl.getId(), result.get().getId());
    }

    @Test
    void testFindByTimestampAfterOrderByTimestampDesc_ReturnsRecentEntries() {
        // Given: Entries at different times
        var now = Instant.now();
        var twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        var oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        repository.save(new SolarIndicesEntity(100.0, 5, 2, 50, twoHoursAgo, SOURCE_NOAA));
        repository.save(new SolarIndicesEntity(120.0, 7, 3, 75, oneHourAgo, SOURCE_NOAA));
        repository.save(new SolarIndicesEntity(150.0, 10, 3, 100, now, SOURCE_NOAA));

        // When: Find entries after 90 minutes ago
        var cutoff = now.minus(90, ChronoUnit.MINUTES);
        var result = repository.findByTimestampAfterOrderByTimestampDesc(cutoff);

        // Then: Should return only recent entries, ordered by timestamp desc
        assertEquals(2, result.size());
        assertEquals(150.0, result.get(0).getSolarFluxIndex()); // Most recent first
        assertEquals(120.0, result.get(1).getSolarFluxIndex());
    }

    @Test
    void testFindTopByOrderByTimestampDesc_ReturnsLatestAcrossAllSources() {
        // Given: Entries from different sources at different times
        var older = Instant.now().minus(1, ChronoUnit.HOURS);
        var newest = Instant.now();

        repository.save(new SolarIndicesEntity(100.0, 5, 2, 50, older, SOURCE_NOAA));
        var latest = repository.save(new SolarIndicesEntity(150.0, 10, 3, 100, newest, SOURCE_HAMQSL));

        // When: Find most recent overall
        var result = repository.findTopByOrderByTimestampDesc();

        // Then: Should return the most recent regardless of source
        assertTrue(result.isPresent());
        assertEquals(latest.getId(), result.get().getId());
    }

    @Test
    void testSaveWithNullSource_ThrowsException() {
        // Given: An entity with null source (required field)
        var entity = new SolarIndicesEntity(150.0, 10, 3, 100, Instant.now(), null);

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testConvertedDomainModel_CanCalculateScore() {
        // Given: A persisted entity
        var entity = repository.save(
                new SolarIndicesEntity(150.0, 10, 3, 100, Instant.now(), SOURCE_NOAA));

        // When: Convert to domain and calculate score
        var domain = entity.toDomain();
        var score = domain.getScore();

        // Then: Score should be calculated correctly
        // High SFI (150), moderate A-index (10), low K-index (3) = good conditions
        assertTrue(score > 50, "Score should be above 50 for good conditions");
        assertTrue(score <= 100, "Score should be at most 100");
    }

    @Test
    void testConvertedDomainModel_CanDetermineIfFavorable() {
        // Given: Favorable conditions (high SFI, low K, low A)
        var favorableEntity = repository.save(
                new SolarIndicesEntity(150.0, 15, 2, 100, Instant.now(), SOURCE_NOAA));

        // When: Convert to domain
        var favorable = favorableEntity.toDomain();

        // Then: Should be favorable (SFI > 100, K < 4, A < 20)
        assertTrue(favorable.isFavorable());
    }

    // === Setter Coverage Tests ===

    @Test
    void testSetters_AllFields_UpdatesEntity() {
        // Given: An entity created via constructor
        var now = Instant.now();
        var entity = new SolarIndicesEntity(150.5, 10, 3, 100, now, SOURCE_NOAA);
        var saved = repository.save(entity);

        // When: Update all fields via setters
        var newTime = Instant.now().plus(1, ChronoUnit.HOURS);
        saved.setSolarFluxIndex(175.0);
        saved.setAIndex(15);
        saved.setKIndex(5);
        saved.setSunspotNumber(120);
        saved.setTimestamp(newTime);
        saved.setSource(SOURCE_HAMQSL);

        // Then: All getters should return updated values
        assertEquals(175.0, saved.getSolarFluxIndex());
        assertEquals(15, saved.getAIndex());
        assertEquals(5, saved.getKIndex());
        assertEquals(120, saved.getSunspotNumber());
        assertEquals(newTime, saved.getTimestamp());
        assertEquals(SOURCE_HAMQSL, saved.getSource());
    }

    private SolarIndices createSampleDomain() {
        return new SolarIndices(150.5, 10, 3, 100, Instant.now(), SOURCE_NOAA);
    }
}
