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

    // ===========================================
    // countSpotsByBandModeInBuckets tests (TimescaleDB time_bucket)
    // ===========================================

    @Test
    void testCountSpotsByBandModeInBuckets_MultipleSpots_ReturnsBucketedCounts() {
        // Two spots in the same 15-minute bucket, one in a different bucket
        Instant bucket1Start = Instant.parse("2023-06-15T12:00:00Z");
        Instant bucket1Spot2 = Instant.parse("2023-06-15T12:05:00Z");
        Instant bucket2Start = Instant.parse("2023-06-15T12:15:00Z");

        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").spottedAt(bucket1Start).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").spottedAt(bucket1Spot2).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").spottedAt(bucket2Start).build())
        ));
        clearPersistenceContext();

        Instant since = bucket1Start.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.countSpotsByBandModeInBuckets(since);

        assertThat(results).isNotEmpty();
        // Should have 2 buckets for 20m/FT8
        long totalCount = results.stream()
                .mapToLong(row -> ((Number) row[3]).longValue())
                .sum();
        assertThat(totalCount).isEqualTo(3);
    }

    @Test
    void testCountSpotsByBandModeInBuckets_MultipleBandModes_GroupsSeparately() {
        Instant time1 = Instant.parse("2023-06-15T12:01:00Z");
        Instant time2 = Instant.parse("2023-06-15T12:02:00Z");
        Instant time3 = Instant.parse("2023-06-15T12:03:00Z");

        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").spottedAt(time1).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("CW").spottedAt(time2).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("40m").mode("FT8").spottedAt(time3).build())
        ));
        clearPersistenceContext();

        Instant since = time1.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.countSpotsByBandModeInBuckets(since);

        // Should have 3 separate groups (20m/FT8, 20m/CW, 40m/FT8)
        assertThat(results).hasSize(3);
    }

    @Test
    void testCountSpotsByBandModeInBuckets_NoSpots_ReturnsEmpty() {
        List<Object[]> results = repository.countSpotsByBandModeInBuckets(BASE_TIME);

        assertThat(results).isEmpty();
    }

    // ===========================================
    // findMaxDxSpotPerBandMode tests (ROW_NUMBER window function)
    // ===========================================

    @Test
    void testFindMaxDxSpotPerBandMode_MultipleDistances_ReturnsMaxPerPair() {
        Instant time1 = Instant.parse("2023-06-15T12:01:00Z");
        Instant time2 = Instant.parse("2023-06-15T12:02:00Z");
        Instant time3 = Instant.parse("2023-06-15T12:03:00Z");

        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").distanceKm(5000)
                        .spottedCall("G3ABC").spotterCall("W1AW")
                        .spottedAt(time1).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").distanceKm(9000)
                        .spottedCall("JA1XYZ").spotterCall("W6ABC")
                        .spottedAt(time2).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("40m").mode("CW").distanceKm(7000)
                        .spottedCall("VK2ABC").spotterCall("K3XYZ")
                        .spottedAt(time3).build())
        ));
        clearPersistenceContext();

        Instant since = time1.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.findMaxDxSpotPerBandMode(since);

        // Should have 2 rows: one for 20m/FT8 (9000km) and one for 40m/CW (7000km)
        assertThat(results).hasSize(2);

        // Find the 20m/FT8 result and verify it picked the max distance
        Object[] ft8Result = results.stream()
                .filter(row -> "20m".equals(row[0]) && "FT8".equals(row[1]))
                .findFirst().orElseThrow();
        assertThat(((Number) ft8Result[2]).intValue()).isEqualTo(9000);
    }

    @Test
    void testFindMaxDxSpotPerBandMode_NoSpots_ReturnsEmpty() {
        List<Object[]> results = repository.findMaxDxSpotPerBandMode(BASE_TIME);

        assertThat(results).isEmpty();
    }

    @Test
    void testFindMaxDxSpotPerBandMode_NullDistance_Excluded() {
        Instant time1 = Instant.parse("2023-06-15T12:01:00Z");

        repository.saveAndFlush(SpotFixtures.spotEntity(SpotFixtures.spot()
                .band("20m").mode("FT8").distanceKm(null)
                .spottedAt(time1).build()));
        clearPersistenceContext();

        Instant since = time1.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.findMaxDxSpotPerBandMode(since);

        assertThat(results).isEmpty();
    }

    // ===========================================
    // countContinentPathsPerBandMode tests
    // ===========================================

    @Test
    void testCountContinentPathsPerBandMode_CrossContinent_CountsPaths() {
        Instant time1 = Instant.parse("2023-06-15T12:01:00Z");
        Instant time2 = Instant.parse("2023-06-15T12:02:00Z");

        repository.saveAllAndFlush(List.of(
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").transAtlantic()
                        .spottedAt(time1).build()),
                SpotFixtures.spotEntity(SpotFixtures.spot()
                        .band("20m").mode("FT8").transAtlantic()
                        .spottedAt(time2).build())
        ));
        clearPersistenceContext();

        Instant since = time1.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.countContinentPathsPerBandMode(since);

        assertThat(results).hasSize(1);
        // [band, mode, spotter_continent, spotted_continent, count]
        assertThat(((Number) results.get(0)[4]).longValue()).isEqualTo(2);
    }

    @Test
    void testCountContinentPathsPerBandMode_SameContinent_Excluded() {
        Instant time1 = Instant.parse("2023-06-15T12:01:00Z");

        repository.saveAndFlush(SpotFixtures.spotEntity(SpotFixtures.spot()
                .band("20m").mode("FT8").local()
                .spottedAt(time1).build()));
        clearPersistenceContext();

        Instant since = time1.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.countContinentPathsPerBandMode(since);

        assertThat(results).isEmpty();
    }

    @Test
    void testCountContinentPathsPerBandMode_NullContinent_Excluded() {
        Instant time1 = Instant.parse("2023-06-15T12:01:00Z");

        repository.saveAndFlush(SpotFixtures.spotEntity(SpotFixtures.spot()
                .band("20m").mode("FT8")
                .spotterContinent(null).spottedContinent("EU")
                .spottedAt(time1).build()));
        clearPersistenceContext();

        Instant since = time1.minus(1, ChronoUnit.HOURS);
        List<Object[]> results = repository.countContinentPathsPerBandMode(since);

        assertThat(results).isEmpty();
    }

    @Test
    void testCountContinentPathsPerBandMode_NoSpots_ReturnsEmpty() {
        List<Object[]> results = repository.countContinentPathsPerBandMode(BASE_TIME);

        assertThat(results).isEmpty();
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
