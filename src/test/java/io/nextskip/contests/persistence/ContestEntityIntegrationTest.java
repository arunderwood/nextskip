package io.nextskip.contests.persistence;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.test.AbstractPersistenceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for ContestEntity and repository operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Entity-to-domain and domain-to-entity conversions</li>
 *   <li>ElementCollection persistence for bands and modes</li>
 *   <li>Repository query methods</li>
 *   <li>Defensive copying of collections</li>
 * </ul>
 */
class ContestEntityIntegrationTest extends AbstractPersistenceTest {

    private static final String ARRL_10M_NAME = "ARRL 10-Meter Contest";
    private static final String CQ_WW_NAME = "CQ WW DX Contest";
    private static final String TEST_CONTEST_NAME = "Test Contest";
    private static final String ALL_BANDS_CONTEST_NAME = "All Bands Contest";
    private static final String MODE_CW = "CW";
    private static final String MODE_SSB = "SSB";
    private static final String MODE_RTTY = "RTTY";
    private static final String MODE_FT8 = "FT8";
    private static final String SPONSOR_ARRL = "ARRL";
    private static final String SPONSOR_CQ = "CQ Magazine";
    private static final String CALENDAR_URL = "https://example.com/calendar";
    private static final String RULES_URL = "https://example.com/rules";

    @Autowired
    private ContestRepository repository;

    @Override
    protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
        return List.of(repository);
    }

    @Test
    void testFromDomain_PreservesAllFields() {
        // Given: A domain model
        var domain = createArrl10mDomain();

        // When: Convert to entity
        var entity = ContestEntity.fromDomain(domain);

        // Then: All fields should be preserved
        assertEquals(ARRL_10M_NAME, entity.getName());
        assertEquals(domain.startTime(), entity.getStartTime());
        assertEquals(domain.endTime(), entity.getEndTime());
        assertEquals(Set.of(FrequencyBand.BAND_10M), entity.getBands());
        assertEquals(Set.of(MODE_CW, MODE_SSB), entity.getModes());
        assertEquals(SPONSOR_ARRL, entity.getSponsor());
        assertEquals(CALENDAR_URL, entity.getCalendarSourceUrl());
        assertEquals(RULES_URL, entity.getOfficialRulesUrl());
    }

    @Test
    void testToDomain_PreservesAllFields() {
        // Given: An entity
        var now = Instant.now();
        var entity = createArrl10mEntity(now);

        // When: Convert to domain
        var domain = entity.toDomain();

        // Then: All fields should be preserved
        assertEquals(ARRL_10M_NAME, domain.name());
        assertEquals(entity.getStartTime(), domain.startTime());
        assertEquals(entity.getEndTime(), domain.endTime());
        assertEquals(Set.of(FrequencyBand.BAND_10M), domain.bands());
        assertEquals(Set.of(MODE_CW, MODE_SSB), domain.modes());
        assertEquals(SPONSOR_ARRL, domain.sponsor());
    }

    @Test
    void testToDomain_DefensiveCopiesCollections() {
        // Given: An entity
        var entity = createArrl10mEntity(Instant.now());
        var domain = entity.toDomain();

        // When: Modify entity's collections
        entity.getModes().add(MODE_FT8);
        entity.getBands().add(FrequencyBand.BAND_20M);

        // Then: Domain's collections should be unchanged
        assertEquals(Set.of(MODE_CW, MODE_SSB), domain.modes());
        assertEquals(Set.of(FrequencyBand.BAND_10M), domain.bands());
    }

    @Test
    void testRoundTrip_DomainToEntityToDomain_PreservesEquality() {
        // Given: A domain model
        var original = createArrl10mDomain();

        // When: Convert to entity and back to domain
        var roundTripped = ContestEntity.fromDomain(original).toDomain();

        // Then: Should be equal to original
        assertEquals(original, roundTripped);
    }

    @Test
    void testSaveAndRetrieve_Success() {
        // Given: A domain model converted to entity
        var domain = createArrl10mDomain();
        var entity = ContestEntity.fromDomain(domain);

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID
        assertNotNull(saved.getId());

        // And: Should be retrievable
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(ARRL_10M_NAME, found.get().getName());
    }

    @Test
    void testSaveWithBandsAndModes_PersistsToJunctionTables() {
        // Given: A contest with multiple bands and modes
        var now = Instant.now();
        var entity = new ContestEntity(
                CQ_WW_NAME,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS),
                Set.of(FrequencyBand.BAND_160M, FrequencyBand.BAND_80M, FrequencyBand.BAND_40M,
                        FrequencyBand.BAND_20M, FrequencyBand.BAND_15M, FrequencyBand.BAND_10M),
                Set.of(MODE_CW, MODE_SSB, MODE_RTTY),
                SPONSOR_CQ,
                null, null
        );

        // When: Save and flush to database
        var saved = repository.saveAndFlush(entity);

        // Clear persistence context to force reload from DB
        entityManager.clear();

        // Then: Should reload with all bands and modes from junction tables
        var reloaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals(6, reloaded.getBands().size());
        assertEquals(3, reloaded.getModes().size());
        assertTrue(reloaded.getBands().contains(FrequencyBand.BAND_20M));
        assertTrue(reloaded.getModes().contains(MODE_CW));
    }

    @Test
    void testSaveWithEmptyCollections_Success() {
        // Given: A contest with empty bands and modes
        var now = Instant.now();
        var entity = new ContestEntity(
                TEST_CONTEST_NAME,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                Set.of(), Set.of(),
                null, null, null
        );

        // When: Save to database
        var saved = repository.saveAndFlush(entity);

        // Then: Should succeed with empty collections
        entityManager.clear();
        var reloaded = repository.findById(saved.getId()).orElseThrow();
        assertTrue(reloaded.getBands().isEmpty());
        assertTrue(reloaded.getModes().isEmpty());
    }

    @Test
    void testFindByStartTimeAfter_ReturnsUpcomingContests() {
        // Given: Multiple contests at different times
        var now = Instant.now();

        // Past contest (already started)
        repository.save(createContestEntity("Past Contest", now.minus(2, ChronoUnit.DAYS)));

        // Upcoming contests
        var upcoming1 = repository.save(createContestEntity("Upcoming Contest 1", now.plus(1, ChronoUnit.DAYS)));
        var upcoming2 = repository.save(createContestEntity("Upcoming Contest 2", now.plus(5, ChronoUnit.DAYS)));

        // When: Find contests starting after now
        var result = repository.findByStartTimeAfterOrderByStartTimeAsc(now);

        // Then: Should return only upcoming contests, ordered by start time
        assertEquals(2, result.size());
        assertEquals(upcoming1.getId(), result.get(0).getId());
        assertEquals(upcoming2.getId(), result.get(1).getId());
    }

    @Test
    void testFindActiveContests_ReturnsCurrentlyActive() {
        // Given: Multiple contests at different times
        var now = Instant.now();

        // Past contest
        repository.save(createContestEntity("Past Contest", now.minus(10, ChronoUnit.DAYS)));

        // Currently active contest
        var active = repository.save(new ContestEntity(
                "Active Contest",
                now.minus(1, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.HOURS),
                Set.of(), Set.of(),
                null, null, null
        ));

        // Future contest
        repository.save(createContestEntity("Future Contest", now.plus(10, ChronoUnit.DAYS)));

        // When: Find active contests (now between start and end)
        var result = repository.findByStartTimeBeforeAndEndTimeAfterOrderByEndTimeAsc(now, now);

        // Then: Should return only the active contest
        assertEquals(1, result.size());
        assertEquals(active.getId(), result.get(0).getId());
    }

    @Test
    void testSaveWithNullName_ThrowsException() {
        // Given: An entity with null name (required field)
        var now = Instant.now();
        var entity = new ContestEntity(
                null,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                Set.of(), Set.of(),
                null, null, null
        );

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testAllFrequencyBands_CanBePersisted() {
        // Given: A contest with all frequency bands
        var now = Instant.now();
        var allBands = Set.of(FrequencyBand.values());
        var entity = new ContestEntity(
                ALL_BANDS_CONTEST_NAME,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                allBands, Set.of(MODE_CW),
                null, null, null
        );

        // When: Save and reload
        var saved = repository.saveAndFlush(entity);
        entityManager.clear();
        var reloaded = repository.findById(saved.getId()).orElseThrow();

        // Then: All bands should be persisted
        assertEquals(FrequencyBand.values().length, reloaded.getBands().size());
        for (FrequencyBand band : FrequencyBand.values()) {
            assertTrue(reloaded.getBands().contains(band), "Should contain band: " + band);
        }
    }

    @Test
    void testDeleteContest_CascadesToJunctionTables() {
        // Given: A contest with bands and modes
        var entity = ContestEntity.fromDomain(createArrl10mDomain());
        var saved = repository.saveAndFlush(entity);
        var contestId = saved.getId();
        assertFalse(saved.getBands().isEmpty());

        // When: Delete the contest
        repository.deleteById(contestId);
        repository.flush();

        // Then: Contest should be deleted (junction tables cascade)
        assertTrue(repository.findById(contestId).isEmpty());
    }

    // === Setter Coverage Tests ===

    @Test
    void testSetters_AllFields_UpdatesEntity() {
        // Given: An entity created via constructor
        var entity = createArrl10mEntity(Instant.now());
        var saved = repository.save(entity);

        // When: Update all fields via setters
        var newStart = Instant.now().plus(1, ChronoUnit.DAYS);
        var newEnd = Instant.now().plus(3, ChronoUnit.DAYS);
        saved.setName("New Contest Name");
        saved.setStartTime(newStart);
        saved.setEndTime(newEnd);
        saved.setBands(Set.of(FrequencyBand.BAND_40M, FrequencyBand.BAND_80M));
        saved.setModes(Set.of(MODE_RTTY, MODE_FT8));
        saved.setSponsor("New Sponsor");
        saved.setCalendarSourceUrl("https://new.example.com/calendar");
        saved.setOfficialRulesUrl("https://new.example.com/rules");

        // Then: All getters should return updated values
        assertEquals("New Contest Name", saved.getName());
        assertEquals(newStart, saved.getStartTime());
        assertEquals(newEnd, saved.getEndTime());
        assertEquals(Set.of(FrequencyBand.BAND_40M, FrequencyBand.BAND_80M), saved.getBands());
        assertEquals(Set.of(MODE_RTTY, MODE_FT8), saved.getModes());
        assertEquals("New Sponsor", saved.getSponsor());
        assertEquals("https://new.example.com/calendar", saved.getCalendarSourceUrl());
        assertEquals("https://new.example.com/rules", saved.getOfficialRulesUrl());
    }

    @Test
    void testSetBands_NullValue_CreatesEmptySet() {
        // Given: An entity with existing bands
        var entity = createArrl10mEntity(Instant.now());
        assertFalse(entity.getBands().isEmpty());

        // When: Set bands to null
        entity.setBands(null);

        // Then: Should have empty set
        assertTrue(entity.getBands().isEmpty());
    }

    @Test
    void testSetModes_NullValue_CreatesEmptySet() {
        // Given: An entity with existing modes
        var entity = createArrl10mEntity(Instant.now());
        assertFalse(entity.getModes().isEmpty());

        // When: Set modes to null
        entity.setModes(null);

        // Then: Should have empty set
        assertTrue(entity.getModes().isEmpty());
    }

    @Test
    void testConstructor_NullBands_CreatesEmptySet() {
        // Given/When: Create entity with null bands
        var now = Instant.now();
        var entity = new ContestEntity(
                TEST_CONTEST_NAME,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                null, Set.of(MODE_CW),
                null, null, null
        );

        // Then: Bands should be empty, not null
        assertNotNull(entity.getBands());
        assertTrue(entity.getBands().isEmpty());
    }

    @Test
    void testConstructor_NullModes_CreatesEmptySet() {
        // Given/When: Create entity with null modes
        var now = Instant.now();
        var entity = new ContestEntity(
                TEST_CONTEST_NAME,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                Set.of(FrequencyBand.BAND_20M), null,
                null, null, null
        );

        // Then: Modes should be empty, not null
        assertNotNull(entity.getModes());
        assertTrue(entity.getModes().isEmpty());
    }

    // Helper methods

    private Contest createArrl10mDomain() {
        var now = Instant.now();
        return new Contest(
                ARRL_10M_NAME,
                now.plus(7, ChronoUnit.DAYS),
                now.plus(9, ChronoUnit.DAYS),
                Set.of(FrequencyBand.BAND_10M),
                Set.of(MODE_CW, MODE_SSB),
                SPONSOR_ARRL,
                CALENDAR_URL,
                RULES_URL
        );
    }

    private ContestEntity createArrl10mEntity(Instant baseTime) {
        return new ContestEntity(
                ARRL_10M_NAME,
                baseTime.plus(7, ChronoUnit.DAYS),
                baseTime.plus(9, ChronoUnit.DAYS),
                Set.of(FrequencyBand.BAND_10M),
                Set.of(MODE_CW, MODE_SSB),
                SPONSOR_ARRL,
                CALENDAR_URL,
                RULES_URL
        );
    }

    private ContestEntity createContestEntity(String name, Instant startTime) {
        return new ContestEntity(
                name,
                startTime,
                startTime.plus(2, ChronoUnit.DAYS),
                Set.of(FrequencyBand.BAND_20M),
                Set.of(MODE_CW),
                null, null, null
        );
    }
}
