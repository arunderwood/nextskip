package io.nextskip.activations.persistence;

import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.activations.model.Summit;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
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
@SuppressWarnings("PMD.TooManyMethods") // Comprehensive test suite
class ActivationEntityIntegrationTest extends AbstractPersistenceTest {

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

    // Spot IDs for de-duplication tests
    private static final String SPOT_ID_1 = "spot-1";
    private static final String SPOT_ID_2 = "spot-2";
    private static final String SPOT_ID_3 = "spot-3";

    // Alternate values for de-duplication tests
    private static final String ALT_PARK_REFERENCE_1 = "K-0001";
    private static final String ALT_PARK_REFERENCE_2 = "K-0002";
    private static final String ALT_PARK_REFERENCE_3 = "K-0818";
    private static final String ALT_CALLSIGN_1 = "W1ABC";
    private static final String ALT_CALLSIGN_2 = "K2DEF";
    private static final String ALT_CALLSIGN_3 = "W1XYZ";
    private static final String ALT_CALLSIGN_4 = "N2OLD";

    @Autowired
    private ActivationRepository repository;

    @Override
    protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
        return List.of(repository);
    }

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

        // And: lastSeenAt should be preserved
        assertEquals(domain.lastSeenAt(), entity.getLastSeenAt());

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

        // Then: lastSeenAt should be preserved
        assertEquals(entity.getLastSeenAt(), domain.lastSeenAt());

        // And: Domain should have Park location
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

        repository.save(createActivationEntity(SPOT_ID_1, now, ALT_CALLSIGN_1));
        repository.save(createActivationEntity(SPOT_ID_2, now, CALLSIGN));
        repository.save(createActivationEntity(SPOT_ID_3, now, CALLSIGN));

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
                FREQUENCY_20M, MODE_FT8, now, now, 15, "OTHER_SOURCE",
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
                FREQUENCY_20M, MODE_FT8, now, now, 15, POTA_SOURCE,
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
                FREQUENCY_20M, MODE_FT8, now, now, 15, null,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE, null
        );

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    // === LastSeenAt vs SpottedAt Tests ===

    @Test
    void testFromDomain_DifferentSpottedAtAndLastSeenAt_PreservesBoth() {
        // Given: An activation with different spottedAt and lastSeenAt (key use case)
        var spottedAt = Instant.parse("2025-01-15T10:00:00Z");
        var lastSeenAt = Instant.parse("2025-01-15T11:30:00Z");
        var park = new Park(PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE);
        var domain = new Activation(
                POTA_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, spottedAt, lastSeenAt, 15, POTA_SOURCE, park
        );

        // When: Convert to entity
        var entity = ActivationEntity.fromDomain(domain);

        // Then: Both timestamps should be preserved independently
        assertEquals(spottedAt, entity.getSpottedAt());
        assertEquals(lastSeenAt, entity.getLastSeenAt());
    }

    @Test
    void testRoundTrip_DifferentSpottedAtAndLastSeenAt_PreservesBoth() {
        // Given: An activation with different spottedAt and lastSeenAt
        var spottedAt = Instant.parse("2025-01-15T10:00:00Z");
        var lastSeenAt = Instant.parse("2025-01-15T11:30:00Z");
        var park = new Park(PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE);
        var original = new Activation(
                POTA_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, spottedAt, lastSeenAt, 15, POTA_SOURCE, park
        );

        // When: Round-trip through entity
        var roundTripped = ActivationEntity.fromDomain(original).toDomain();

        // Then: Both timestamps should be preserved
        assertEquals(spottedAt, roundTripped.spottedAt());
        assertEquals(lastSeenAt, roundTripped.lastSeenAt());
        assertEquals(original, roundTripped);
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

    // === Setter Coverage Tests ===

    @Test
    void testSetters_AllFields_UpdatesEntity() {
        // Given: An entity created via constructor
        var now = Instant.now();
        var entity = createPotaEntity(now);
        var saved = repository.save(entity);

        // When: Update all fields via setters
        var newTime = Instant.now();
        var newLastSeenAt = newTime.plusSeconds(300);
        saved.setSpotId("new-spot-id");
        saved.setActivatorCallsign("W9XYZ");
        saved.setType(ActivationType.SOTA);
        saved.setFrequency(21.0);
        saved.setMode(MODE_CW);
        saved.setSpottedAt(newTime);
        saved.setLastSeenAt(newLastSeenAt);
        saved.setQsoCount(25);
        saved.setSource("NEW_SOURCE");
        saved.setLocationReference("NEW-REF");
        saved.setLocationName("New Location");
        saved.setLocationRegionCode("TX");
        saved.setParkCountryCode("CA");
        saved.setParkGrid("EM10");
        saved.setParkLatitude(35.0);
        saved.setParkLongitude(-100.0);
        saved.setSummitAssociationCode("W5T");

        // Then: All getters should return updated values
        assertEquals("new-spot-id", saved.getSpotId());
        assertEquals("W9XYZ", saved.getActivatorCallsign());
        assertEquals(ActivationType.SOTA, saved.getType());
        assertEquals(21.0, saved.getFrequency());
        assertEquals(MODE_CW, saved.getMode());
        assertEquals(newTime, saved.getSpottedAt());
        assertEquals(newLastSeenAt, saved.getLastSeenAt());
        assertEquals(25, saved.getQsoCount());
        assertEquals("NEW_SOURCE", saved.getSource());
        assertEquals("NEW-REF", saved.getLocationReference());
        assertEquals("New Location", saved.getLocationName());
        assertEquals("TX", saved.getLocationRegionCode());
        assertEquals("CA", saved.getParkCountryCode());
        assertEquals("EM10", saved.getParkGrid());
        assertEquals(35.0, saved.getParkLatitude());
        assertEquals(-100.0, saved.getParkLongitude());
        assertEquals("W5T", saved.getSummitAssociationCode());
    }

    // Helper methods

    private Activation createPotaDomain() {
        var now = Instant.now();
        var park = new Park(PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE);
        return new Activation(
                POTA_SPOT_ID, CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, now, now, 15, POTA_SOURCE, park
        );
    }

    private Activation createSotaDomain() {
        var now = Instant.now();
        var summit = new Summit(SUMMIT_REFERENCE, SUMMIT_NAME, SUMMIT_REGION, SUMMIT_ASSOCIATION);
        return new Activation(
                SOTA_SPOT_ID, CALLSIGN, ActivationType.SOTA,
                FREQUENCY_40M, MODE_CW, now, now, 5, SOTA_SOURCE, summit
        );
    }

    private ActivationEntity createPotaEntity(Instant spottedAt) {
        return new ActivationEntity(
                POTA_SPOT_ID + "-" + spottedAt.toEpochMilli(), CALLSIGN, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, spottedAt, spottedAt, 15, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, PARK_LATITUDE, PARK_LONGITUDE, null
        );
    }

    private ActivationEntity createSotaEntity(Instant spottedAt) {
        return new ActivationEntity(
                SOTA_SPOT_ID + "-" + spottedAt.toEpochMilli(), CALLSIGN, ActivationType.SOTA,
                FREQUENCY_40M, MODE_CW, spottedAt, spottedAt, 5, SOTA_SOURCE,
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
                FREQUENCY_20M, MODE_FT8, spottedAt, spottedAt, 10, POTA_SOURCE,
                PARK_REFERENCE, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, TEST_LAT, TEST_LON, null
        );
    }

    private ActivationEntity createActivationEntity(
            String spotId, Instant spottedAt, String callsign, String locationRef) {
        return new ActivationEntity(
                spotId, callsign, ActivationType.POTA,
                FREQUENCY_20M, MODE_FT8, spottedAt, spottedAt, 10, POTA_SOURCE,
                locationRef, PARK_NAME, PARK_REGION,
                PARK_COUNTRY, PARK_GRID, TEST_LAT, TEST_LON, null
        );
    }

    // === De-duplication Query Tests ===

    @Test
    void testFindLatestPerCallsignAndLocation_MultipleSpotsSameGroup_ReturnsOnlyMostRecent() {
        // Given: Same callsign at same location, different times
        var now = Instant.now();
        repository.save(createActivationEntity(
                "spot-old", now.minus(5, ChronoUnit.MINUTES), CALLSIGN, PARK_REFERENCE));
        var mostRecent = repository.save(createActivationEntity(
                "spot-new", now.minus(1, ChronoUnit.MINUTES), CALLSIGN, PARK_REFERENCE));

        // When: Query de-duplicates by (callsign, location)
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findLatestPerCallsignAndLocation(cutoff);

        // Then: Only the most recent spot should be returned
        assertEquals(1, result.size());
        assertEquals(mostRecent.getId(), result.get(0).getId());
    }

    @Test
    void testFindLatestPerCallsignAndLocation_DifferentLocations_ReturnsBoth() {
        // Given: Same callsign at different locations
        var now = Instant.now();
        var spot1 = repository.save(createActivationEntity(
                SPOT_ID_1, now.minus(2, ChronoUnit.MINUTES), CALLSIGN, ALT_PARK_REFERENCE_1));
        var spot2 = repository.save(createActivationEntity(
                SPOT_ID_2, now.minus(1, ChronoUnit.MINUTES), CALLSIGN, ALT_PARK_REFERENCE_2));

        // When: Query de-duplicates by (callsign, location)
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findLatestPerCallsignAndLocation(cutoff);

        // Then: Both should be returned (different locations)
        assertEquals(2, result.size());
        var resultIds = result.stream().map(ActivationEntity::getId).toList();
        assertTrue(resultIds.contains(spot1.getId()));
        assertTrue(resultIds.contains(spot2.getId()));
    }

    @Test
    void testFindLatestPerCallsignAndLocation_DifferentCallsigns_ReturnsBoth() {
        // Given: Different callsigns at same location
        var now = Instant.now();
        var spot1 = repository.save(createActivationEntity(
                SPOT_ID_1, now.minus(2, ChronoUnit.MINUTES), ALT_CALLSIGN_1, PARK_REFERENCE));
        var spot2 = repository.save(createActivationEntity(
                SPOT_ID_2, now.minus(1, ChronoUnit.MINUTES), ALT_CALLSIGN_2, PARK_REFERENCE));

        // When: Query de-duplicates by (callsign, location)
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findLatestPerCallsignAndLocation(cutoff);

        // Then: Both should be returned (different callsigns)
        assertEquals(2, result.size());
        var resultIds = result.stream().map(ActivationEntity::getId).toList();
        assertTrue(resultIds.contains(spot1.getId()));
        assertTrue(resultIds.contains(spot2.getId()));
    }

    @Test
    void testFindLatestPerCallsignAndLocation_OutsideTimeWindow_Excluded() {
        // Given: Activation outside the time window
        var now = Instant.now();
        repository.save(createActivationEntity(
                "spot-old", now.minus(35, ChronoUnit.MINUTES), CALLSIGN, PARK_REFERENCE));

        // When: Query with 30-minute cutoff
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findLatestPerCallsignAndLocation(cutoff);

        // Then: Should be excluded
        assertEquals(0, result.size());
    }

    @Test
    void testFindLatestPerCallsignAndLocation_MixedScenario_CorrectDeduplication() {
        // Given: Multiple scenarios combined:
        // - CALLSIGN at PARK_REFERENCE: 3 spots (keep most recent)
        // - CALLSIGN at ALT_PARK_REFERENCE_3: 1 spot (keep)
        // - ALT_CALLSIGN_3 at PARK_REFERENCE: 2 spots (keep most recent)
        // - ALT_CALLSIGN_4 at ALT_PARK_REFERENCE_1: 1 old spot (exclude)
        var now = Instant.now();

        // CALLSIGN at PARK_REFERENCE (3 spots, keep most recent)
        repository.save(createActivationEntity(
                SPOT_ID_1, now.minus(10, ChronoUnit.MINUTES), CALLSIGN, PARK_REFERENCE));
        repository.save(createActivationEntity(
                SPOT_ID_2, now.minus(5, ChronoUnit.MINUTES), CALLSIGN, PARK_REFERENCE));
        var callsignAtPark = repository.save(createActivationEntity(
                SPOT_ID_3, now.minus(1, ChronoUnit.MINUTES), CALLSIGN, PARK_REFERENCE));

        // CALLSIGN at ALT_PARK_REFERENCE_3 (1 spot)
        var callsignAtAltPark = repository.save(createActivationEntity(
                "spot-4", now.minus(3, ChronoUnit.MINUTES), CALLSIGN, ALT_PARK_REFERENCE_3));

        // ALT_CALLSIGN_3 at PARK_REFERENCE (2 spots, keep most recent)
        repository.save(createActivationEntity(
                "spot-5", now.minus(8, ChronoUnit.MINUTES), ALT_CALLSIGN_3, PARK_REFERENCE));
        var altCallsignAtPark = repository.save(createActivationEntity(
                "spot-6", now.minus(2, ChronoUnit.MINUTES), ALT_CALLSIGN_3, PARK_REFERENCE));

        // ALT_CALLSIGN_4 at ALT_PARK_REFERENCE_1 (old spot, outside window)
        repository.save(createActivationEntity(
                "spot-7", now.minus(45, ChronoUnit.MINUTES), ALT_CALLSIGN_4, ALT_PARK_REFERENCE_1));

        // When: Query with 30-minute cutoff
        var cutoff = now.minus(30, ChronoUnit.MINUTES);
        var result = repository.findLatestPerCallsignAndLocation(cutoff);

        // Then: Should have exactly 3 results (3 unique callsign+location pairs within window)
        assertEquals(3, result.size());
        var resultIds = result.stream().map(ActivationEntity::getId).toList();
        assertTrue(resultIds.contains(callsignAtPark.getId()), "Should include CALLSIGN at PARK_REFERENCE");
        assertTrue(resultIds.contains(callsignAtAltPark.getId()), "Should include CALLSIGN at ALT_PARK_REFERENCE_3");
        assertTrue(resultIds.contains(altCallsignAtPark.getId()), "Should include ALT_CALLSIGN_3 at PARK_REFERENCE");
    }
}
