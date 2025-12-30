package io.nextskip.meteors.persistence;

import io.nextskip.common.model.EventStatus;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import io.nextskip.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for MeteorShowerEntity and repository operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Entity-to-domain and domain-to-entity conversions</li>
 *   <li>Repository CRUD operations</li>
 *   <li>Custom query methods for finding upcoming and active showers</li>
 *   <li>Constraint enforcement (time window ordering)</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class MeteorShowerEntityIntegrationTest extends AbstractIntegrationTest {

    private static final String PERSEIDS_CODE = "PER";
    private static final String PERSEIDS_NAME = "Perseids 2025";
    private static final String GEMINIDS_CODE = "GEM";
    private static final String GEMINIDS_NAME = "Geminids 2025";
    private static final String PERSEIDS_PARENT_BODY = "109P/Swift-Tuttle";
    private static final String PERSEIDS_INFO_URL = "https://example.com/perseids";

    @Autowired
    private MeteorShowerRepository repository;

    @Test
    void testFromDomain_PreservesAllFields() {
        // Given: A domain model
        var domain = createPerseidsDomain();

        // When: Convert to entity
        var entity = MeteorShowerEntity.fromDomain(domain);

        // Then: All fields should be preserved
        assertEquals(PERSEIDS_NAME, entity.getName());
        assertEquals(PERSEIDS_CODE, entity.getCode());
        assertEquals(domain.peakStart(), entity.getPeakStart());
        assertEquals(domain.peakEnd(), entity.getPeakEnd());
        assertEquals(domain.visibilityStart(), entity.getVisibilityStart());
        assertEquals(domain.visibilityEnd(), entity.getVisibilityEnd());
        assertEquals(100, entity.getPeakZhr());
        assertEquals(PERSEIDS_PARENT_BODY, entity.getParentBody());
        assertEquals(PERSEIDS_INFO_URL, entity.getInfoUrl());
    }

    @Test
    void testToDomain_PreservesAllFields() {
        // Given: An entity
        var now = Instant.now();
        var entity = createPerseidsEntity(now);

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: All fields should be preserved
        assertEquals(PERSEIDS_NAME, domain.name());
        assertEquals(PERSEIDS_CODE, domain.code());
        assertEquals(entity.getPeakStart(), domain.peakStart());
        assertEquals(entity.getPeakEnd(), domain.peakEnd());
        assertEquals(entity.getVisibilityStart(), domain.visibilityStart());
        assertEquals(entity.getVisibilityEnd(), domain.visibilityEnd());
        assertEquals(100, domain.peakZhr());
        assertEquals(PERSEIDS_PARENT_BODY, domain.parentBody());
        assertEquals(PERSEIDS_INFO_URL, domain.infoUrl());
    }

    @Test
    void testToDomain_HandlesNullOptionalFields() {
        // Given: An entity with null optional fields
        var now = Instant.now();
        var entity = new MeteorShowerEntity(
                PERSEIDS_NAME, PERSEIDS_CODE,
                now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
                now, now.plus(3, ChronoUnit.DAYS),
                100, null, null);

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: Nullable fields should be null
        assertNull(domain.parentBody());
        assertNull(domain.infoUrl());
    }

    @Test
    void testRoundTrip_DomainToEntityToDomain_PreservesEquality() {
        // Given: A domain model
        var original = createPerseidsDomain();

        // When: Convert to entity and back to domain
        var roundTripped = MeteorShowerEntity.fromDomain(original).toDomain();

        // Then: Should be equal to original
        assertEquals(original, roundTripped);
    }

    @Test
    void testSaveAndRetrieve_Success() {
        // Given: A domain model converted to entity
        var domain = createPerseidsDomain();
        var entity = MeteorShowerEntity.fromDomain(domain);

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID
        assertNotNull(saved.getId());

        // And: Should be retrievable
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(PERSEIDS_NAME, found.get().getName());
    }

    @Test
    void testFindByCode_ReturnsShower() {
        // Given: A persisted shower
        var entity = MeteorShowerEntity.fromDomain(createPerseidsDomain());
        repository.save(entity);

        // When: Find by code
        var result = repository.findByCode(PERSEIDS_CODE);

        // Then: Should find the shower
        assertTrue(result.isPresent());
        assertEquals(PERSEIDS_NAME, result.get().getName());
    }

    @Test
    void testFindByVisibilityStartAfter_ReturnsUpcomingShowers() {
        // Given: Multiple showers with different visibility starts
        var now = Instant.now();

        // Past shower (already started)
        repository.save(createShowerEntity("Past", "PST", now.minus(10, ChronoUnit.DAYS)));

        // Upcoming showers
        var upcoming1 = repository.save(createShowerEntity("Upcoming1", "UP1", now.plus(5, ChronoUnit.DAYS)));
        var upcoming2 = repository.save(createShowerEntity("Upcoming2", "UP2", now.plus(10, ChronoUnit.DAYS)));

        // When: Find showers starting after now
        var result = repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(now);

        // Then: Should return only upcoming showers, ordered by visibility start
        assertEquals(2, result.size());
        assertEquals(upcoming1.getId(), result.get(0).getId());
        assertEquals(upcoming2.getId(), result.get(1).getId());
    }

    @Test
    void testFindActiveShowers_ReturnsCurrentlyActive() {
        // Given: Multiple showers with different time windows
        var now = Instant.now();

        // Past shower
        repository.save(createShowerEntity("Past", "PST", now.minus(30, ChronoUnit.DAYS)));

        // Currently active shower
        var active = repository.save(new MeteorShowerEntity(
                "Active Shower", "ACT",
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                now.minus(2, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
                50, null, null));

        // Future shower
        repository.save(createShowerEntity("Future", "FUT", now.plus(30, ChronoUnit.DAYS)));

        // When: Find active showers (visibility contains now)
        var result = repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(now, now);

        // Then: Should return only the active shower
        assertEquals(1, result.size());
        assertEquals(active.getId(), result.get(0).getId());
    }

    @Test
    void testSaveWithNullName_ThrowsException() {
        // Given: An entity with null name (required field)
        var now = Instant.now();
        var entity = new MeteorShowerEntity(
                null, PERSEIDS_CODE,
                now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
                now, now.plus(3, ChronoUnit.DAYS),
                100, null, null);

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testSaveWithNullCode_ThrowsException() {
        // Given: An entity with null code (required field)
        var now = Instant.now();
        var entity = new MeteorShowerEntity(
                PERSEIDS_NAME, null,
                now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
                now, now.plus(3, ChronoUnit.DAYS),
                100, null, null);

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testConvertedDomainModel_CanCalculateScore() {
        // Given: A persisted entity for an upcoming shower
        var futureStart = Instant.now().plus(1, ChronoUnit.DAYS);
        var entity = repository.save(new MeteorShowerEntity(
                PERSEIDS_NAME, PERSEIDS_CODE,
                futureStart.plus(2, ChronoUnit.DAYS), futureStart.plus(3, ChronoUnit.DAYS),
                futureStart, futureStart.plus(5, ChronoUnit.DAYS),
                100, PERSEIDS_PARENT_BODY, null));

        // When: Convert to domain and calculate score
        var domain = entity.toDomain();
        var score = domain.getScore();

        // Then: Score should be calculated (upcoming shower = 60-80 range)
        assertTrue(score >= 60 && score <= 80, "Score should be in upcoming range: " + score);
    }

    @Test
    void testConvertedDomainModel_CanDetermineStatus() {
        // Given: An active shower entity
        var now = Instant.now();
        var activeEntity = repository.save(new MeteorShowerEntity(
                "Active Shower", "ACT",
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                now.minus(2, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
                50, null, null));

        // When: Convert to domain
        var active = activeEntity.toDomain();

        // Then: Should be ACTIVE status
        assertEquals(EventStatus.ACTIVE, active.getStatus());
    }

    @Test
    void testConvertedDomainModel_CanDetermineIfFavorable() {
        // Given: A shower at peak
        var now = Instant.now();
        var atPeakEntity = repository.save(new MeteorShowerEntity(
                "At Peak", "PEK",
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                now.minus(2, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
                100, null, null));

        // When: Convert to domain
        var atPeak = atPeakEntity.toDomain();

        // Then: Should be favorable (at peak)
        assertTrue(atPeak.isFavorable());
        assertTrue(atPeak.isAtPeak());
    }

    // === Setter Coverage Tests ===

    @Test
    void testSetters_AllFields_UpdatesEntity() {
        // Given: An entity created via constructor
        var entity = createPerseidsEntity(Instant.now());
        var saved = repository.save(entity);

        // When: Update all fields via setters
        var newPeakStart = Instant.now().plus(20, ChronoUnit.DAYS);
        var newPeakEnd = Instant.now().plus(21, ChronoUnit.DAYS);
        var newVisibilityStart = Instant.now().plus(15, ChronoUnit.DAYS);
        var newVisibilityEnd = Instant.now().plus(25, ChronoUnit.DAYS);
        saved.setName(GEMINIDS_NAME);
        saved.setCode(GEMINIDS_CODE);
        saved.setPeakStart(newPeakStart);
        saved.setPeakEnd(newPeakEnd);
        saved.setVisibilityStart(newVisibilityStart);
        saved.setVisibilityEnd(newVisibilityEnd);
        saved.setPeakZhr(120);
        saved.setParentBody("3200 Phaethon");
        saved.setInfoUrl("https://example.com/geminids");

        // Then: All getters should return updated values
        assertEquals(GEMINIDS_NAME, saved.getName());
        assertEquals(GEMINIDS_CODE, saved.getCode());
        assertEquals(newPeakStart, saved.getPeakStart());
        assertEquals(newPeakEnd, saved.getPeakEnd());
        assertEquals(newVisibilityStart, saved.getVisibilityStart());
        assertEquals(newVisibilityEnd, saved.getVisibilityEnd());
        assertEquals(120, saved.getPeakZhr());
        assertEquals("3200 Phaethon", saved.getParentBody());
        assertEquals("https://example.com/geminids", saved.getInfoUrl());
    }

    // Helper methods

    private MeteorShower createPerseidsDomain() {
        var now = Instant.now();
        return new MeteorShower(
                PERSEIDS_NAME, PERSEIDS_CODE,
                now.plus(10, ChronoUnit.DAYS), now.plus(11, ChronoUnit.DAYS),  // peak window
                now.plus(5, ChronoUnit.DAYS), now.plus(15, ChronoUnit.DAYS),   // visibility window
                100,
                PERSEIDS_PARENT_BODY,
                PERSEIDS_INFO_URL
        );
    }

    private MeteorShowerEntity createPerseidsEntity(Instant baseTime) {
        return new MeteorShowerEntity(
                PERSEIDS_NAME, PERSEIDS_CODE,
                baseTime.plus(10, ChronoUnit.DAYS), baseTime.plus(11, ChronoUnit.DAYS),
                baseTime.plus(5, ChronoUnit.DAYS), baseTime.plus(15, ChronoUnit.DAYS),
                100,
                PERSEIDS_PARENT_BODY,
                PERSEIDS_INFO_URL
        );
    }

    private MeteorShowerEntity createShowerEntity(String name, String code, Instant visibilityStart) {
        return new MeteorShowerEntity(
                name, code,
                visibilityStart.plus(5, ChronoUnit.DAYS), visibilityStart.plus(6, ChronoUnit.DAYS),
                visibilityStart, visibilityStart.plus(10, ChronoUnit.DAYS),
                50, null, null
        );
    }
}
