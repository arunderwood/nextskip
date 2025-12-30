package io.nextskip.activations.persistence;

import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.activations.model.Summit;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.test.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for ActivationEntity and repository operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Entity-to-domain and domain-to-entity conversions</li>
 *   <li>Denormalized location field handling for POTA (Park) and SOTA (Summit)</li>
 *   <li>Unique constraint enforcement on (spot_id, source)</li>
 *   <li>Repository query methods</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@SuppressWarnings("PMD.TooManyMethods") // Comprehensive test suite
class ActivationEntityIntegrationTest extends AbstractIntegrationTest {

    private static final String POTA_SPOT_ID = "pota-spot-12345";
    private static final String SOTA_SPOT_ID = "sota-spot-67890";
    private static final String CALLSIGN = "K0ABC";
    private static final String POTA_SOURCE = "POTA";
    private static final String SOTA_SOURCE = "SOTA";

    // Location constants for POTA parks
    private static final String PARK_REFERENCE = "K-0817";
    private static final String PARK_NAME = "Rocky Mountain National Park";
    private static final String PARK_REGION = "CO";
    private static final String PARK_COUNTRY = "US";
    private static final String PARK_GRID = "DM79";
    private static final double PARK_LATITUDE = 40.343;
    private static final double PARK_LONGITUDE = -105.684;

    // Location constants for SOTA summits
    private static final String SUMMIT_REFERENCE = "W7W/LC-001";
    private static final String SUMMIT_NAME = "Tiger Mountain";
    private static final String SUMMIT_REGION = "WA";
    private static final String SUMMIT_ASSOCIATION = "W7W";

    // Common test data
    private static final String MODE_FT8 = "FT8";
    private static final String MODE_CW = "CW";
    private static final double FREQUENCY_20M = 14.074;
    private static final double FREQUENCY_40M = 7.030;
    private static final double TEST_LAT = 40.0;
    private static final double TEST_LON = -105.0;

    // Test-specific spot IDs
    private static final String FRESH_SPOT_ID = "fresh-spot";
    private static final String STALE_SPOT_ID = "stale-spot";
    private static final String ALT_PARK_REFERENCE = "K-0818";
    private static final String ALT_PARK_NAME = "Another Park";

    @Autowired
    private ActivationRepository repository;

    @Autowired
    private EntityManager entityManager;

    // === POTA (Park) Conversion Tests ===

    @Test
    void testFromDomain_PotaActivation_PreservesAllFields() {
        // Given: A POTA domain model with Park location
        var domain = createPotaDomain();

        // When: Convert to entity
        var entity = ActivationEntity.fromDomain(domain);

        // Then: All activation fields should be preserved
        assertEquals(POTA_SPOT_ID, entity.getSpotId());
        assertEquals(CALLSIGN, entity.getActivatorCallsign());
        assertEquals(ActivationType.POTA, entity.getType());
        assertEquals(FREQUENCY_20M, entity.getFrequency());
        assertEquals(MODE_FT8, entity.getMode());
        assertEquals(domain.spottedAt(), entity.getSpottedAt());
        assertEquals(15, entity.getQsoCount());
        assertEquals(POTA_SOURCE, entity.getSource());

        // And: Common location fields should be preserved
        assertEquals(PARK_REFERENCE, entity.getLocationReference());
        assertEquals(PARK_NAME, entity.getLocationName());
        assertEquals(PARK_REGION, entity.getLocationRegionCode());

        // And: Park-specific fields should be preserved
        assertEquals(PARK_COUNTRY, entity.getParkCountryCode());
        assertEquals(PARK_GRID, entity.getParkGrid());
        assertEquals(PARK_LATITUDE, entity.getParkLatitude());
        assertEquals(PARK_LONGITUDE, entity.getParkLongitude());

        // And: Summit-specific fields should be null
        assertNull(entity.getSummitAssociationCode());
    }

    @Test
    void testToDomain_PotaActivation_ReconstructsParkCorrectly() {
        // Given: A POTA entity
        var entity = createPotaEntity(Instant.now());

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: Domain should have Park location
        assertTrue(domain.location() instanceof Park);
        var park = (Park) domain.location();

        assertEquals(PARK_REFERENCE, park.reference());
        assertEquals(PARK_NAME, park.name());
        assertEquals(PARK_REGION, park.regionCode());
        assertEquals(PARK_COUNTRY, park.countryCode());
        assertEquals(PARK_GRID, park.grid());
        assertEquals(PARK_LATITUDE, park.latitude());
        assertEquals(PARK_LONGITUDE, park.longitude());
    }

    // === SOTA (Summit) Conversion Tests ===

    @Test
    void testFromDomain_SotaActivation_PreservesAllFields() {
        // Given: A SOTA domain model with Summit location
        var domain = createSotaDomain();

        // When: Convert to entity
        var entity = ActivationEntity.fromDomain(domain);

        // Then: All activation fields should be preserved
        assertEquals(SOTA_SPOT_ID, entity.getSpotId());
        assertEquals(CALLSIGN, entity.getActivatorCallsign());
        assertEquals(ActivationType.SOTA, entity.getType());
        assertEquals(FREQUENCY_40M, entity.getFrequency());
        assertEquals(MODE_CW, entity.getMode());
        assertEquals(SOTA_SOURCE, entity.getSource());

        // And: Common location fields should be preserved
        assertEquals(SUMMIT_REFERENCE, entity.getLocationReference());
        assertEquals(SUMMIT_NAME, entity.getLocationName());
        assertEquals(SUMMIT_REGION, entity.getLocationRegionCode());

        // And: Summit-specific fields should be preserved
        assertEquals(SUMMIT_ASSOCIATION, entity.getSummitAssociationCode());

        // And: Park-specific fields should be null
        assertNull(entity.getParkCountryCode());
        assertNull(entity.getParkGrid());
        assertNull(entity.getParkLatitude());
        assertNull(entity.getParkLongitude());
    }

    @Test
    void testToDomain_SotaActivation_ReconstructsSummitCorrectly() {
        // Given: A SOTA entity
        var entity = createSotaEntity(Instant.now());

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: Domain should have Summit location
        assertTrue(domain.location() instanceof Summit);
        var summit = (Summit) domain.location();

        assertEquals(SUMMIT_REFERENCE, summit.reference());
        assertEquals(SUMMIT_NAME, summit.name());
        assertEquals(SUMMIT_REGION, summit.regionCode());
        assertEquals(SUMMIT_ASSOCIATION, summit.associationCode());
    }

    // === Round-Trip Tests ===

    @Test
    void testRoundTrip_PotaActivation_PreservesEquality() {
        // Given: A POTA domain model
        var original = createPotaDomain();

        // When: Convert to entity and back to domain
        var roundTripped = ActivationEntity.fromDomain(original).toDomain();

        // Then: Should be equal to original
        assertEquals(original, roundTripped);
    }

    @Test
    void testRoundTrip_SotaActivation_PreservesEquality() {
        // Given: A SOTA domain model
        var original = createSotaDomain();

        // When: Convert to entity and back to domain
        var roundTripped = ActivationEntity.fromDomain(original).toDomain();

        // Then: Should be equal to original
        assertEquals(original, roundTripped);
    }

    // === Repository Tests ===

    @Test
    void testSaveAndRetrieve_PotaActivation_Success() {
        // Given: A POTA domain model converted to entity
        var domain = createPotaDomain();
        var entity = ActivationEntity.fromDomain(domain);

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID
        assertNotNull(saved.getId());

        // And: Should be retrievable
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(ActivationType.POTA, found.get().getType());
    }

    @Test
    void testSaveAndRetrieve_SotaActivation_Success() {
        // Given: A SOTA domain model converted to entity
        var domain = createSotaDomain();
        var entity = ActivationEntity.fromDomain(domain);

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID and be retrievable
        assertNotNull(saved.getId());
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(ActivationType.SOTA, found.get().getType());
    }

    @Test
    void testFindBySpotIdAndSource_ReturnsActivation() {
        // Given: A persisted activation
        var entity = ActivationEntity.fromDomain(createPotaDomain());
        repository.save(entity);

        // When: Find by spot ID and source
        var result = repository.findBySpotIdAndSource(POTA_SPOT_ID, POTA_SOURCE);

        // Then: Should find the activation
        assertTrue(result.isPresent());
        assertEquals(CALLSIGN, result.get().getActivatorCallsign());
    }

    @Test
    void testFindBySpottedAtAfter_ReturnsRecentActivations() {
        // Given: Multiple activations at different times
        var now = Instant.now();

        // Old activation
        repository.save(createActivationEntity("old-spot", now.minus(2, ChronoUnit.HOURS)));

        // Recent activations
        var recent1 = repository.save(createActivationEntity("recent-1", now.minus(5, ChronoUnit.MINUTES)));
        var recent2 = repository.save(createActivationEntity("recent-2", now.minus(2, ChronoUnit.MINUTES)));

        // When: Find activations spotted after cutoff
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findBySpottedAtAfterOrderBySpottedAtDesc(cutoff);

        // Then: Should return only recent activations, most recent first
        assertEquals(2, result.size());
        assertEquals(recent2.getId(), result.get(0).getId());
        assertEquals(recent1.getId(), result.get(1).getId());
    }

    @Test
    void testFindByTypeAndSpottedAtAfter_FiltersCorrectly() {
        // Given: Mix of POTA and SOTA activations
        var now = Instant.now();

        var pota1 = repository.save(createPotaEntity(now.minus(5, ChronoUnit.MINUTES)));
        repository.save(createSotaEntity(now.minus(3, ChronoUnit.MINUTES)));
        var pota2 = repository.save(createPotaEntity(now.minus(1, ChronoUnit.MINUTES)));

        // When: Find only POTA activations
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                ActivationType.POTA, cutoff);

        // Then: Should return only POTA activations
        assertEquals(2, result.size());
        assertEquals(pota2.getId(), result.get(0).getId());
        assertEquals(pota1.getId(), result.get(1).getId());
    }

    @Test
    void testFindByActivatorCallsign_MatchingCallsign_ReturnsActivations() {
        // Given: Activations from different operators
        var now = Instant.now();

        repository.save(createActivationEntity("spot-1", now, "W1ABC"));
        repository.save(createActivationEntity("spot-2", now, CALLSIGN));
        repository.save(createActivationEntity("spot-3", now, CALLSIGN));

        // When: Find by callsign
        var result = repository.findByActivatorCallsignOrderBySpottedAtDesc(CALLSIGN);

        // Then: Should return only activations by this operator
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(a -> CALLSIGN.equals(a.getActivatorCallsign())));
    }

    // === Constraint Tests ===

    @Test
    void testUniqueConstraint_DuplicateSpotIdSource_ThrowsException() {
        // Given: A persisted activation
        var entity1 = ActivationEntity.fromDomain(createPotaDomain());
        repository.saveAndFlush(entity1);

        // When: Try to save another with same spot_id and source
        var entity2 = ActivationEntity.fromDomain(createPotaDomain());

        // Then: Should throw exception
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity2));
    }

    @Test
    void testUniqueConstraint_SameSpotIdDifferentSource_Succeeds() {
        // Given: An activation from POTA source
        var potaEntity = ActivationEntity.fromDomain(createPotaDomain());
        repository.save(potaEntity);

        // When: Save activation with same spot ID but different source
        var now = Instant.now();
        var otherSourceEntity = new ActivationEntity(
                POTA_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now, 15, "OTHER_SOURCE",
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE, null
        );
        var saved = repository.saveAndFlush(otherSourceEntity);

        // Then: Should succeed
        assertNotNull(saved.getId());
    }

    @Test
    void testSaveWithNullSpotId_NullValue_ThrowsException() {
        // Given: An entity with null spot ID (required field)
        var now = Instant.now();
        var entity = new ActivationEntity(
                null, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now, 15, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE, null
        );

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testSaveWithNullSource_NullValue_ThrowsException() {
        // Given: An entity with null source (required field)
        var now = Instant.now();
        var entity = new ActivationEntity(
                POTA_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now, 15, null,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE, null
        );

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    // === Domain Model Behavior Tests ===

    @Test
    void testConvertedDomainModel_CanCalculateScore() {
        // Given: A recently spotted activation
        var now = Instant.now();
        var entity = repository.save(new ActivationEntity(
                FRESH_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now.minus(3, ChronoUnit.MINUTES), 10, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, TEST_LAT, TEST_LON, null
        ));

        // When: Convert to domain and calculate score
        var domain = entity.toDomain();
        var score = domain.getScore();

        // Then: Fresh activation should have score of 100
        assertEquals(100, score);
    }

    @Test
    void testConvertedDomainModel_CanDetermineIfFavorable() {
        // Given: A recently spotted activation (within 15 minutes)
        var now = Instant.now();
        var freshEntity = repository.save(new ActivationEntity(
                FRESH_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now.minus(10, ChronoUnit.MINUTES), 10, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, TEST_LAT, TEST_LON, null
        ));

        // And: An old activation
        var staleEntity = repository.save(new ActivationEntity(
                STALE_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now.minus(45, ChronoUnit.MINUTES), 10, POTA_SOURCE,
                ALT_PARK_REFERENCE, ALT_PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, TEST_LAT, TEST_LON, null
        ));

        // When: Convert to domain
        var fresh = freshEntity.toDomain();
        var stale = staleEntity.toDomain();

        // Then: Fresh should be favorable, stale should not
        assertTrue(fresh.isFavorable());
        assertFalse(stale.isFavorable());
    }

    @Test
    void testSave_PotaEntity_PersistsAndReloadsCorrectly() {
        // Given: A POTA entity
        var entity = ActivationEntity.fromDomain(createPotaDomain());

        // When: Save, clear persistence context, and reload
        var saved = repository.saveAndFlush(entity);
        entityManager.clear();

        // Then: Reloaded entity should have all fields
        var reloaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals(PARK_REFERENCE, reloaded.getLocationReference());
        assertEquals(PARK_COUNTRY, reloaded.getParkCountryCode());
        assertEquals(PARK_LATITUDE, reloaded.getParkLatitude());
    }

    // Helper methods

    private Activation createPotaDomain() {
        var now = Instant.now();
        var park = new Park(PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE);
        return new Activation(
                POTA_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now, 15, POTA_SOURCE, park
        );
    }

    private Activation createSotaDomain() {
        var now = Instant.now();
        var summit = new Summit(SUMMIT_REFERENCE, SUMMIT_NAME, SUMMIT_REGION, SUMMIT_ASSOCIATION);
        return new Activation(
                SOTA_SPOT_ID, CALLSIGN, ActivationType.SOTA,
                FREQUENCY_40M, MODE_CW, now, 5, SOTA_SOURCE, summit
        );
    }

    private ActivationEntity createPotaEntity(Instant spottedAt) {
        return new ActivationEntity(
                POTA_SPOT_ID + "-" + spottedAt.toEpochMilli(), CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, spottedAt, 15, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE, null
        );
    }

    private ActivationEntity createSotaEntity(Instant spottedAt) {
        return new ActivationEntity(
                SOTA_SPOT_ID + "-" + spottedAt.toEpochMilli(), CALLSIGN, ActivationType.SOTA,
                FREQUENCY_40M, MODE_CW, spottedAt, 5, SOTA_SOURCE,
                SUMMIT_REFERENCE, SUMMIT_NAME, SUMMIT_REGION,
                null, null, null, null, SUMMIT_ASSOCIATION
        );
    }

    private ActivationEntity createActivationEntity(String spotId, Instant spottedAt) {
        return createActivationEntity(spotId, spottedAt, CALLSIGN);
    }

    private ActivationEntity createActivationEntity(String spotId, Instant spottedAt, String callsign) {
        return new ActivationEntity(
                spotId, callsign, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, spottedAt, 10, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, TEST_LAT, TEST_LON, null
        );
    }
}
