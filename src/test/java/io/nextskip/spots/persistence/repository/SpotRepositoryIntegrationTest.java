package io.nextskip.spots.persistence.repository;

import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.test.AbstractPersistenceTest;
import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpotRepository}.
 *
 * <p>Tests CRUD operations and query methods using TestContainers PostgreSQL
 * with TimescaleDB extension. The spots table is a hypertable partitioned
 * by {@code spotted_at}.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated band/mode values
class SpotRepositoryIntegrationTest extends AbstractPersistenceTest {

    private static final Instant BASE_TIME = Instant.parse("2023-06-15T12:00:00Z");

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
        SpotEntity entity = SpotFixtures.defaultSpotEntity();

        SpotEntity saved = repository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void testSave_AllFields_Persisted() {
        Instant spottedAt = BASE_TIME.truncatedTo(ChronoUnit.MICROS);
        Spot spot = SpotFixtures.spot()
                .band("40m")
                .mode("CW")
                .frequencyHz(7025000L)
                .snr(-15)
                .spottedAt(spottedAt)
                .spotterCall("W1AW")
                .spotterGrid("FN31")
                .spotterContinent("NA")
                .spottedCall("G3ABC")
                .spottedGrid("JO01")
                .spottedContinent("EU")
                .distanceKm(5500)
                .build();
        SpotEntity entity = SpotFixtures.spotEntity(spot);

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
                SpotFixtures.spotEntity(SpotFixtures.spot().band("20m")
                        .spottedAt(BASE_TIME).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("40m")
                        .spottedAt(BASE_TIME.plusSeconds(1)).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot().band("80m")
                        .spottedAt(BASE_TIME.plusSeconds(2)).build())
        );

        List<SpotEntity> saved = repository.saveAllAndFlush(entities);

        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(e -> e.getId() != null);
        assertThat(repository.count()).isEqualTo(3);
    }

    // ===========================================
    // findByBandAndSpottedAtAfter tests
    // ===========================================

    @Test
    void testFindByBandAndSpottedAtAfter_MatchingSpots_ReturnsInOrder() {
        Instant spottedAt1 = BASE_TIME.minus(1, ChronoUnit.HOURS);
        Instant spottedAt2 = BASE_TIME.minus(30, ChronoUnit.MINUTES);
        Instant spottedAt3 = BASE_TIME;
        Instant spottedAt4 = BASE_TIME.plusSeconds(1); // unique spottedAt since it's the @Id

        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("20m").spottedAt(spottedAt1).spotterCall("first").build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("20m").spottedAt(spottedAt2).spotterCall("second").build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("20m").spottedAt(spottedAt3).spotterCall("third").build()),
                // Different band — must have unique spottedAt (it's the @Id)
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().band("40m").spottedAt(spottedAt4).build())
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
                SpotFixtures.spot().band("20m").spottedAt(BASE_TIME.minus(5, ChronoUnit.HOURS)).build()
        ));

        Instant cutoff = BASE_TIME.minus(1, ChronoUnit.HOURS);
        List<SpotEntity> results = repository.findByBandAndSpottedAtAfterOrderBySpottedAtDesc("20m", cutoff);

        assertThat(results).isEmpty();
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
                        SpotFixtures.spot().spottedAt(twoHoursAgo).spotterCall("older").build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME).spotterCall("newest").build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(oneHourAgo).spotterCall("middle").build())
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
    // countBySpottedAtAfter tests
    // ===========================================

    @Test
    void testCountBySpottedAtAfter_MixedTimes_CountsOnlyRecent() {
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME.minus(2, ChronoUnit.HOURS)).build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME).build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME.plusSeconds(1)).build())
        ));

        Instant cutoff = BASE_TIME.minus(1, ChronoUnit.HOURS);
        long count = repository.countBySpottedAtAfter(cutoff);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountBySpottedAtAfter_AllOld_ReturnsZero() {
        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME.minus(2, ChronoUnit.HOURS)).build()),
                SpotFixtures.spotEntity(
                        SpotFixtures.spot().spottedAt(BASE_TIME.minus(3, ChronoUnit.HOURS)).build())
        ));

        Instant cutoff = BASE_TIME.minus(1, ChronoUnit.HOURS);
        long count = repository.countBySpottedAtAfter(cutoff);

        assertThat(count).isZero();
    }

    // ===========================================
    // Domain conversion round-trip tests
    // ===========================================

    @Test
    void testDomainRoundTrip_SaveAndConvert_PreservesData() {
        // Use truncated timestamp to match PostgreSQL precision (microseconds)
        Instant fixedSpottedAt = BASE_TIME.truncatedTo(ChronoUnit.MICROS);
        Spot original = SpotFixtures.spot()
                .band("17m")
                .mode("FT4")
                .frequencyHz(18100000L)
                .snr(5)
                .spottedAt(fixedSpottedAt)
                .spotterCall("N5XX")
                .spotterGrid("EM13")
                .spotterContinent("NA")
                .spottedCall("LU1ABC")
                .spottedGrid("GF05")
                .spottedContinent("SA")
                .distanceKm(9000)
                .build();

        SpotEntity entity = SpotFixtures.spotEntity(original);
        SpotEntity saved = repository.saveAndFlush(entity);
        clearPersistenceContext();

        SpotEntity reloaded = repository.findById(saved.getId()).orElseThrow();
        Spot converted = reloaded.toDomain();

        assertThat(converted).isEqualTo(original);
    }
}
