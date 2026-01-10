package io.nextskip.contests.persistence;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.persistence.entity.ContestSeriesEntity;
import io.nextskip.contests.persistence.repository.ContestSeriesRepository;
import io.nextskip.test.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for ContestSeriesEntity and repository operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Entity persistence with junction tables for bands and modes</li>
 *   <li>Repository query methods (findByWa7bnmRef)</li>
 *   <li>Unique constraint on wa7bnmRef</li>
 *   <li>Defensive copying of collections</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class ContestSeriesEntityIntegrationTest extends AbstractIntegrationTest {

    private static final String INDIANA_QSO_PARTY_REF = "8";
    private static final String INDIANA_QSO_PARTY_NAME = "Indiana QSO Party";
    private static final String CQ_WW_REF = "10";
    private static final String CQ_WW_NAME = "CQ WW DX Contest";
    private static final String TEST_SERIES_REF = "99";
    private static final String TEST_SERIES_NAME = "Test Series";
    private static final String MODE_CW = "CW";
    private static final String MODE_SSB = "SSB";
    private static final String MODE_DIGITAL = "Digital";
    private static final String SPONSOR_HDXA = "HDXA";
    private static final String SPONSOR_CQ = "CQ Magazine";
    private static final String RULES_URL = "https://example.com/rules";
    private static final String EXCHANGE = "RS(T) + county";
    private static final String CABRILLO_NAME = "IN-QSO-PARTY";

    @Autowired
    private ContestSeriesRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testSaveAndRetrieve_Success() {
        // Given: A contest series entity
        var entity = createIndianaQsoPartySeries();

        // When: Save to database
        var saved = repository.save(entity);

        // Then: Should have generated ID
        assertNotNull(saved.getId());

        // And: Should be retrievable
        var found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(INDIANA_QSO_PARTY_NAME, found.get().getName());
        assertEquals(INDIANA_QSO_PARTY_REF, found.get().getWa7bnmRef());
    }

    @Test
    void testFindByWa7bnmRef_Found_ReturnsEntity() {
        // Given: A saved series
        var entity = createIndianaQsoPartySeries();
        repository.save(entity);

        // When: Find by wa7bnmRef
        var found = repository.findByWa7bnmRef(INDIANA_QSO_PARTY_REF);

        // Then: Should be found
        assertTrue(found.isPresent());
        assertEquals(INDIANA_QSO_PARTY_NAME, found.get().getName());
    }

    @Test
    void testFindByWa7bnmRef_NotFound_ReturnsEmpty() {
        // When: Find by non-existent ref
        var found = repository.findByWa7bnmRef("non-existent");

        // Then: Should be empty
        assertTrue(found.isEmpty());
    }

    @Test
    void testSaveWithBandsAndModes_PersistsToJunctionTables() {
        // Given: A series with multiple bands and modes
        var entity = new ContestSeriesEntity(
                CQ_WW_REF,
                CQ_WW_NAME,
                Set.of(FrequencyBand.BAND_160M, FrequencyBand.BAND_80M, FrequencyBand.BAND_40M,
                        FrequencyBand.BAND_20M, FrequencyBand.BAND_15M, FrequencyBand.BAND_10M),
                Set.of(MODE_CW, MODE_SSB),
                SPONSOR_CQ,
                RULES_URL,
                "RST + CQ Zone",
                "CQ-WW-CW",
                LocalDate.of(2025, 10, 1),
                Instant.now()
        );

        // When: Save and flush to database
        var saved = repository.saveAndFlush(entity);

        // Clear persistence context to force reload from DB
        entityManager.clear();

        // Then: Should reload with all bands and modes from junction tables
        var reloaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals(6, reloaded.getBands().size());
        assertEquals(2, reloaded.getModes().size());
        assertTrue(reloaded.getBands().contains(FrequencyBand.BAND_20M));
        assertTrue(reloaded.getModes().contains(MODE_CW));
    }

    @Test
    void testSaveWithEmptyCollections_Success() {
        // Given: A series with empty bands and modes
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                TEST_SERIES_NAME,
                Set.of(), Set.of(),
                null, null, null, null,
                null, null
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
    void testSaveWithNullWa7bnmRef_ThrowsException() {
        // Given: An entity with null wa7bnmRef (required field)
        var entity = new ContestSeriesEntity(
                null,
                TEST_SERIES_NAME,
                Set.of(), Set.of(),
                null, null, null, null,
                null, null
        );

        // When/Then: Should throw exception on save
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity));
    }

    @Test
    void testSaveDuplicateWa7bnmRef_ThrowsException() {
        // Given: An existing series
        repository.saveAndFlush(createIndianaQsoPartySeries());
        entityManager.clear();

        // When: Try to save another with same wa7bnmRef
        var duplicate = new ContestSeriesEntity(
                INDIANA_QSO_PARTY_REF,  // Same ref
                "Different Name",
                Set.of(), Set.of(),
                null, null, null, null,
                null, null
        );

        // Then: Should throw unique constraint violation
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void testAllFrequencyBands_CanBePersisted() {
        // Given: A series with all frequency bands
        var allBands = Set.of(FrequencyBand.values());
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                TEST_SERIES_NAME,
                allBands, Set.of(MODE_CW),
                null, null, null, null,
                null, null
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
    void testDeleteSeries_CascadesToJunctionTables() {
        // Given: A series with bands and modes
        var entity = createIndianaQsoPartySeries();
        var saved = repository.saveAndFlush(entity);
        var seriesId = saved.getId();
        assertFalse(saved.getBands().isEmpty());

        // When: Delete the series
        repository.deleteById(seriesId);
        repository.flush();

        // Then: Series should be deleted (junction tables cascade)
        assertTrue(repository.findById(seriesId).isEmpty());
    }

    @Test
    void testRevisionDate_PersistsCorrectly() {
        // Given: A series with a revision date
        var revisionDate = LocalDate.of(2025, 11, 1);
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                TEST_SERIES_NAME,
                Set.of(), Set.of(),
                null, null, null, null,
                revisionDate, null
        );

        // When: Save and reload
        var saved = repository.saveAndFlush(entity);
        entityManager.clear();
        var reloaded = repository.findById(saved.getId()).orElseThrow();

        // Then: Revision date should be preserved
        assertEquals(revisionDate, reloaded.getRevisionDate());
    }

    @Test
    void testLastScrapedAt_PersistsCorrectly() {
        // Given: A series with lastScrapedAt timestamp
        var lastScrapedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                TEST_SERIES_NAME,
                Set.of(), Set.of(),
                null, null, null, null,
                null, lastScrapedAt
        );

        // When: Save and reload
        var saved = repository.saveAndFlush(entity);
        entityManager.clear();
        var reloaded = repository.findById(saved.getId()).orElseThrow();

        // Then: LastScrapedAt should be preserved (truncated to microseconds by DB)
        assertNotNull(reloaded.getLastScrapedAt());
        // Compare with tolerance for database timestamp precision
        assertTrue(Math.abs(lastScrapedAt.toEpochMilli() - reloaded.getLastScrapedAt().toEpochMilli()) < 1000);
    }

    @Test
    void testNullOptionalFields_PersistsCorrectly() {
        // Given: A series with only required fields
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                null,  // name is optional
                Set.of(), Set.of(),
                null, null, null, null,
                null, null
        );

        // When: Save and reload
        var saved = repository.saveAndFlush(entity);
        entityManager.clear();
        var reloaded = repository.findById(saved.getId()).orElseThrow();

        // Then: All optional fields should be null
        assertNull(reloaded.getName());
        assertNull(reloaded.getSponsor());
        assertNull(reloaded.getOfficialRulesUrl());
        assertNull(reloaded.getExchange());
        assertNull(reloaded.getCabrilloName());
        assertNull(reloaded.getRevisionDate());
        assertNull(reloaded.getLastScrapedAt());
    }

    // === Setter Coverage Tests ===

    @Test
    void testSetters_AllFields_UpdatesEntity() {
        // Given: An entity created via constructor
        var entity = createIndianaQsoPartySeries();
        var saved = repository.save(entity);

        // When: Update all fields via setters
        var newRevisionDate = LocalDate.of(2025, 12, 15);
        var newLastScrapedAt = Instant.now();
        saved.setWa7bnmRef("new-ref");
        saved.setName("New Series Name");
        saved.setBands(Set.of(FrequencyBand.BAND_160M, FrequencyBand.BAND_80M));
        saved.setModes(Set.of(MODE_DIGITAL));
        saved.setSponsor("New Sponsor");
        saved.setOfficialRulesUrl("https://new.example.com/rules");
        saved.setExchange("New Exchange");
        saved.setCabrilloName("NEW-CONTEST");
        saved.setRevisionDate(newRevisionDate);
        saved.setLastScrapedAt(newLastScrapedAt);

        // Then: All getters should return updated values
        assertEquals("new-ref", saved.getWa7bnmRef());
        assertEquals("New Series Name", saved.getName());
        assertEquals(Set.of(FrequencyBand.BAND_160M, FrequencyBand.BAND_80M), saved.getBands());
        assertEquals(Set.of(MODE_DIGITAL), saved.getModes());
        assertEquals("New Sponsor", saved.getSponsor());
        assertEquals("https://new.example.com/rules", saved.getOfficialRulesUrl());
        assertEquals("New Exchange", saved.getExchange());
        assertEquals("NEW-CONTEST", saved.getCabrilloName());
        assertEquals(newRevisionDate, saved.getRevisionDate());
        assertEquals(newLastScrapedAt, saved.getLastScrapedAt());
    }

    @Test
    void testSetBands_NullValue_CreatesEmptySet() {
        // Given: An entity with existing bands
        var entity = createIndianaQsoPartySeries();
        assertFalse(entity.getBands().isEmpty());

        // When: Set bands to null
        entity.setBands(null);

        // Then: Should have empty set
        assertTrue(entity.getBands().isEmpty());
    }

    @Test
    void testSetModes_NullValue_CreatesEmptySet() {
        // Given: An entity with existing modes
        var entity = createIndianaQsoPartySeries();
        assertFalse(entity.getModes().isEmpty());

        // When: Set modes to null
        entity.setModes(null);

        // Then: Should have empty set
        assertTrue(entity.getModes().isEmpty());
    }

    @Test
    void testConstructor_NullBands_CreatesEmptySet() {
        // Given/When: Create entity with null bands
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                TEST_SERIES_NAME,
                null, Set.of(MODE_CW),
                null, null, null, null,
                null, null
        );

        // Then: Bands should be empty, not null
        assertNotNull(entity.getBands());
        assertTrue(entity.getBands().isEmpty());
    }

    @Test
    void testConstructor_NullModes_CreatesEmptySet() {
        // Given/When: Create entity with null modes
        var entity = new ContestSeriesEntity(
                TEST_SERIES_REF,
                TEST_SERIES_NAME,
                Set.of(FrequencyBand.BAND_20M), null,
                null, null, null, null,
                null, null
        );

        // Then: Modes should be empty, not null
        assertNotNull(entity.getModes());
        assertTrue(entity.getModes().isEmpty());
    }

    @Test
    void testUpdate_ExistingSeries_PersistsChanges() {
        // Given: An existing series in the database
        var entity = createIndianaQsoPartySeries();
        var saved = repository.saveAndFlush(entity);
        var seriesId = saved.getId();
        entityManager.clear();

        // When: Reload and update
        var reloaded = repository.findById(seriesId).orElseThrow();
        reloaded.setSponsor("Updated Sponsor");
        reloaded.setBands(Set.of(FrequencyBand.BAND_10M));
        reloaded.setRevisionDate(LocalDate.of(2025, 12, 25));
        repository.saveAndFlush(reloaded);
        entityManager.clear();

        // Then: Changes should be persisted
        var updated = repository.findById(seriesId).orElseThrow();
        assertEquals("Updated Sponsor", updated.getSponsor());
        assertEquals(Set.of(FrequencyBand.BAND_10M), updated.getBands());
        assertEquals(LocalDate.of(2025, 12, 25), updated.getRevisionDate());
    }

    // Helper methods

    private ContestSeriesEntity createIndianaQsoPartySeries() {
        return new ContestSeriesEntity(
                INDIANA_QSO_PARTY_REF,
                INDIANA_QSO_PARTY_NAME,
                Set.of(FrequencyBand.BAND_40M, FrequencyBand.BAND_20M),
                Set.of(MODE_CW, MODE_SSB),
                SPONSOR_HDXA,
                RULES_URL,
                EXCHANGE,
                CABRILLO_NAME,
                LocalDate.of(2025, 11, 1),
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
    }
}
