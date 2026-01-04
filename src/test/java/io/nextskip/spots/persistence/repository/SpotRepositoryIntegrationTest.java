package io.nextskip.spots.persistence.repository;

import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.test.AbstractPersistenceTest;
import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.nextskip.test.TestConstants.SPOT_TTL_HOURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpotRepository}.
 *
 * <p>Tests CRUD operations, query methods, and TTL cleanup using TestContainers PostgreSQL.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated band/mode values
class SpotRepositoryIntegrationTest extends AbstractPersistenceTest {

    private static final Instant BASE_TIME = Instant.parse("2023-06-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(BASE_TIME, ZoneId.of("UTC"));

    @Autowired
    private SpotRepository repository;

    @Override
    protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
        return List.of(repository);
    }

    // ===========================================
    // Basic CRUD tests
    // ===========================================

    @Test
    void testSave_NewEntity_AssignsId() {
        SpotEntity entity = SpotFixtures.defaultSpotEntity(FIXED_CLOCK);

        SpotEntity saved = repository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void testSave_AllFields_Persisted() {
        Spot spot = SpotFixtures.spot()
                .band("40m")
                .mode("CW")
                .frequencyHz(7025000L)
                .snr(-15)
                .spotterCall("W1AW")
                .spotterGrid("FN31")
                .spotterContinent("NA")
                .spottedCall("G3ABC")
                .spottedGrid("JO01")
                .spottedContinent("EU")
                .distanceKm(5500)
                .build();
        SpotEntity entity = SpotFixtures.spotEntity(spot, FIXED_CLOCK);

        SpotEntity saved = repository.saveAndFlush(entity);
        clearPersistenceContext();

        SpotEntity found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getBand()).isEqualTo("40m");
        assertThat(found.getMode()).isEqualTo("CW");
        assertThat(found.getFrequencyHz()).isEqualTo(7025000L);
        assertThat(found.getSnr()).isEqualTo(-15);
        assertThat(found.getSpotterCall()).isEqualTo("W1AW");
        assertThat(found.getSpotterGrid()).isEqualTo("FN31");
        assertThat(found.getSpotterContinent()).isEqualTo("NA");
        assertThat(found.getSpottedCall()).isEqualTo("G3ABC");
        assertThat(found.getSpottedGrid()).isEqualTo("JO01");
        assertThat(found.getSpottedContinent()).isEqualTo("EU");
        assertThat(found.getDistanceKm()).isEqualTo(5500);
    }

    @Test
    void testSaveAll_BatchInsert_AllPersisted() {
        List<SpotEntity> entities = List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").build(), FIXED_CLOCK),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("40m").build(), FIXED_CLOCK),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("80m").build(), FIXED_CLOCK)
        );

        List<SpotEntity> saved = repository.saveAllAndFlush(entities);

        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(e -> e.getId() != null);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void testFindById_ExistingEntity_ReturnsEntity() {
        SpotEntity entity = repository.saveAndFlush(SpotFixtures.defaultSpotEntity(FIXED_CLOCK));
        clearPersistenceContext();

        Optional<SpotEntity> found = repository.findById(entity.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBand()).isEqualTo(entity.getBand());
    }

    @Test
    void testFindById_NonExistingEntity_ReturnsEmpty() {
        Optional<SpotEntity> found = repository.findById(999999L);

        assertThat(found).isEmpty();
    }

    @Test
    void testDelete_ExistingEntity_Removed() {
        SpotEntity entity = repository.saveAndFlush(SpotFixtures.defaultSpotEntity(FIXED_CLOCK));
        Long id = entity.getId();

        repository.deleteById(id);
        repository.flush();
        clearPersistenceContext();

        assertThat(repository.findById(id)).isEmpty();
    }

    // ===========================================
    // deleteByCreatedAtBefore tests
    // ===========================================

    @Test
    void testDeleteByCreatedAtBefore_OlderEntities_Deleted() {
        // Create old and new entities
        Clock oldClock = Clock.fixed(BASE_TIME.minus(48, ChronoUnit.HOURS), ZoneId.of("UTC"));
        Clock newClock = Clock.fixed(BASE_TIME, ZoneId.of("UTC"));

        SpotEntity oldSpot = SpotFixtures.spotEntity(
                SpotFixtures.spot().band("old").build(), oldClock);
        SpotEntity newSpot = SpotFixtures.spotEntity(
                SpotFixtures.spot().band("new").build(), newClock);

        repository.saveAllAndFlush(List.of(oldSpot, newSpot));
        clearPersistenceContext();

        Instant cutoff = BASE_TIME.minus(SPOT_TTL_HOURS, ChronoUnit.HOURS);
        int deleted = repository.deleteByCreatedAtBefore(cutoff);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().get(0).getBand()).isEqualTo("new");
    }

    @Test
    void testDeleteByCreatedAtBefore_NoOldEntities_NoneDeleted() {
        Clock recentClock = Clock.fixed(BASE_TIME, ZoneId.of("UTC"));
        repository.saveAndFlush(SpotFixtures.defaultSpotEntity(recentClock));

        Instant cutoff = BASE_TIME.minus(SPOT_TTL_HOURS, ChronoUnit.HOURS);
        int deleted = repository.deleteByCreatedAtBefore(cutoff);

        assertThat(deleted).isZero();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void testDeleteByCreatedAtBefore_AllOld_AllDeleted() {
        Clock oldClock = Clock.fixed(BASE_TIME.minus(48, ChronoUnit.HOURS), ZoneId.of("UTC"));
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").build(), oldClock),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("40m").build(), oldClock)
        ));

        Instant cutoff = BASE_TIME.minus(SPOT_TTL_HOURS, ChronoUnit.HOURS);
        int deleted = repository.deleteByCreatedAtBefore(cutoff);

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.count()).isZero();
    }

    // ===========================================
    // findByBandAndSpottedAtAfter tests
    // ===========================================

    @Test
    void testFindByBandAndSpottedAtAfter_MatchingSpots_ReturnsInOrder() {
        Instant spottedAt1 = BASE_TIME.minus(1, ChronoUnit.HOURS);
        Instant spottedAt2 = BASE_TIME.minus(30, ChronoUnit.MINUTES);
        Instant spottedAt3 = BASE_TIME;

        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("20m").spottedAt(spottedAt1).spotterCall("first").build(),
                        FIXED_CLOCK),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("20m").spottedAt(spottedAt2).spotterCall("second").build(),
                        FIXED_CLOCK),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("20m").spottedAt(spottedAt3).spotterCall("third").build(),
                        FIXED_CLOCK),
                // Different band
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("40m").spottedAt(spottedAt3).build(),
                        FIXED_CLOCK)
        ));
        clearPersistenceContext();

        Instant cutoff = BASE_TIME.minus(2, ChronoUnit.HOURS);
        List<SpotEntity> results = repository.findByBandAndSpottedAtAfterOrderBySpottedAtDesc("20m", cutoff);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getSpotterCall()).isEqualTo("third");
        assertThat(results.get(1).getSpotterCall()).isEqualTo("second");
        assertThat(results.get(2).getSpotterCall()).isEqualTo("first");
    }

    @Test
    void testFindByBandAndSpottedAtAfter_NoMatches_ReturnsEmpty() {
        repository.saveAndFlush(SpotFixtures.spotEntity(
                SpotFixtures.spot().band("20m").spottedAt(BASE_TIME.minus(5, ChronoUnit.HOURS)).build(),
                FIXED_CLOCK
        ));

        Instant cutoff = BASE_TIME.minus(1, ChronoUnit.HOURS);
        List<SpotEntity> results = repository.findByBandAndSpottedAtAfterOrderBySpottedAtDesc("20m", cutoff);

        assertThat(results).isEmpty();
    }

    // ===========================================
    // findByModeAndSpottedAtAfter tests
    // ===========================================

    @Test
    void testFindByModeAndSpottedAtAfter_MatchingSpots_ReturnsFiltered() {
        Instant halfHourAgo = BASE_TIME.minus(30, ChronoUnit.MINUTES);
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().mode("FT8").spottedAt(BASE_TIME).build(),
                        FIXED_CLOCK),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().mode("FT8").spottedAt(halfHourAgo).build(),
                        FIXED_CLOCK),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().mode("CW").spottedAt(BASE_TIME).build(),
                        FIXED_CLOCK)
        ));
        clearPersistenceContext();

        Instant cutoff = BASE_TIME.minus(2, ChronoUnit.HOURS);
        List<SpotEntity> results = repository.findByModeAndSpottedAtAfterOrderBySpottedAtDesc("FT8", cutoff);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(e -> "FT8".equals(e.getMode()));
    }

    // ===========================================
    // findTopByOrderBySpottedAtDesc tests
    // ===========================================

    @Test
    void testFindTopByOrderBySpottedAtDesc_MultipleSpots_ReturnsMostRecent() {
        Instant twoHoursAgo = BASE_TIME.minus(2, ChronoUnit.HOURS);
        Instant oneHourAgo = BASE_TIME.minus(1, ChronoUnit.HOURS);
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(twoHoursAgo).spotterCall("older").build(),
                        FIXED_CLOCK),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME).spotterCall("newest").build(),
                        FIXED_CLOCK),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(oneHourAgo).spotterCall("middle").build(),
                        FIXED_CLOCK)
        ));
        clearPersistenceContext();

        Optional<SpotEntity> result = repository.findTopByOrderBySpottedAtDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getSpotterCall()).isEqualTo("newest");
    }

    @Test
    void testFindTopByOrderBySpottedAtDesc_EmptyRepository_ReturnsEmpty() {
        Optional<SpotEntity> result = repository.findTopByOrderBySpottedAtDesc();

        assertThat(result).isEmpty();
    }

    // ===========================================
    // countByCreatedAtAfter tests
    // ===========================================

    @Test
    void testCountByCreatedAtAfter_MixedTimes_CountsOnlyRecent() {
        Clock oldClock = Clock.fixed(BASE_TIME.minus(2, ChronoUnit.HOURS), ZoneId.of("UTC"));
        Clock newClock = Clock.fixed(BASE_TIME, ZoneId.of("UTC"));

        repository.saveAllAndFlush(List.of(
                SpotFixtures.defaultSpotEntity(oldClock),
                SpotFixtures.defaultSpotEntity(newClock),
                SpotFixtures.defaultSpotEntity(newClock)
        ));

        Instant cutoff = BASE_TIME.minus(1, ChronoUnit.HOURS);
        long count = repository.countByCreatedAtAfter(cutoff);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountByCreatedAtAfter_AllOld_ReturnsZero() {
        Clock oldClock = Clock.fixed(BASE_TIME.minus(2, ChronoUnit.HOURS), ZoneId.of("UTC"));
        repository.saveAllAndFlush(List.of(
                SpotFixtures.defaultSpotEntity(oldClock),
                SpotFixtures.defaultSpotEntity(oldClock)
        ));

        Instant cutoff = BASE_TIME.minus(1, ChronoUnit.HOURS);
        long count = repository.countByCreatedAtAfter(cutoff);

        assertThat(count).isZero();
    }

    // ===========================================
    // findTopByBandAndDistanceKmNotNullOrderByDistanceKmDesc tests
    // ===========================================

    @Test
    void testFindTopByBand_OrderedByDistance_ReturnsFarthest() {
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").distanceKm(3000).build(), FIXED_CLOCK),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").distanceKm(8000).build(), FIXED_CLOCK),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").distanceKm(5500).build(), FIXED_CLOCK),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("40m").distanceKm(10000).build(), FIXED_CLOCK)
        ));
        clearPersistenceContext();

        List<SpotEntity> results = repository.findTopByBandAndDistanceKmNotNullOrderByDistanceKmDesc("20m");

        assertThat(results).isNotEmpty();
        // First result should be the one with highest distance for band 20m
        assertThat(results.get(0).getDistanceKm()).isEqualTo(8000);
    }

    @Test
    void testFindTopByBand_NullDistances_ExcludesNulls() {
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").distanceKm(null).build(), FIXED_CLOCK),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m").distanceKm(5000).build(), FIXED_CLOCK)
        ));
        clearPersistenceContext();

        List<SpotEntity> results = repository.findTopByBandAndDistanceKmNotNullOrderByDistanceKmDesc("20m");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDistanceKm()).isEqualTo(5000);
    }

    // ===========================================
    // Domain conversion round-trip tests
    // ===========================================

    @Test
    void testDomainRoundTrip_SaveAndConvert_PreservesData() {
        Spot original = SpotFixtures.spot()
                .band("17m")
                .mode("FT4")
                .frequencyHz(18100000L)
                .snr(5)
                .spotterCall("N5XX")
                .spotterGrid("EM13")
                .spotterContinent("NA")
                .spottedCall("LU1ABC")
                .spottedGrid("GF05")
                .spottedContinent("SA")
                .distanceKm(9000)
                .build();

        SpotEntity entity = SpotFixtures.spotEntity(original, FIXED_CLOCK);
        SpotEntity saved = repository.saveAndFlush(entity);
        clearPersistenceContext();

        SpotEntity reloaded = repository.findById(saved.getId()).orElseThrow();
        Spot converted = reloaded.toDomain();

        assertThat(converted).isEqualTo(original);
    }
}
