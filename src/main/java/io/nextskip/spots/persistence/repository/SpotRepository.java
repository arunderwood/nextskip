package io.nextskip.spots.persistence.repository;

import io.nextskip.spots.persistence.entity.SpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PSKReporter spots.
 *
 * <p>The spots table is a TimescaleDB hypertable partitioned by {@code spotted_at}.
 * Retention is handled by application-level {@code drop_chunks()} calls
 * (TimescaleDB's {@code add_retention_policy} requires a Timescale license).
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Batch insert via inherited {@code saveAll()}</li>
 *   <li>Bulk aggregation queries for band activity dashboard</li>
 *   <li>Status/health queries</li>
 * </ul>
 */
@Repository
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // JPQL parameter names are intentionally repeated
public interface SpotRepository extends JpaRepository<SpotEntity, Long> {

    /**
     * Finds recent spots by band, ordered by spotted time descending.
     *
     * @param band      the band (e.g., "20m", "40m")
     * @param spottedAt minimum spotted_at time
     * @return list of spots, most recent first
     */
    List<SpotEntity> findByBandAndSpottedAtAfterOrderBySpottedAtDesc(String band, Instant spottedAt);

    /**
     * Finds the most recent spot in the database.
     *
     * <p>Used for status/health reporting.
     *
     * @return the most recent spot, if any
     */
    Optional<SpotEntity> findTopByOrderBySpottedAtDesc();

    /**
     * Counts spots spotted after the specified time.
     *
     * <p>Used for rate metrics and startup detection.
     *
     * @param spottedAt minimum spotted_at time
     * @return count of spots
     */
    long countBySpottedAtAfter(Instant spottedAt);

    // ========================================================================
    // Retention: drop old hypertable chunks (Apache-licensed alternative to
    // add_retention_policy which requires Timescale license)
    // ========================================================================

    /**
     * Drops hypertable chunks older than 6 hours.
     *
     * <p>Uses TimescaleDB's {@code drop_chunks()} which is available under
     * the Apache license (unlike {@code add_retention_policy()}).
     *
     * <p>The interval is hardcoded because {@code INTERVAL} literals cannot
     * be parameterized in PostgreSQL (Hibernate converts parameters to
     * {@code $1}, producing invalid SQL {@code INTERVAL $1}).
     *
     * <p>Called by {@code SpotChunkCleanupTask} on a recurring schedule.
     */
    @Modifying
    @Transactional
    @Query(value = "SELECT drop_chunks('spots', INTERVAL '6 hours')", nativeQuery = true)
    void dropOldChunks();

    // ========================================================================
    // Bulk aggregation queries (replace N+1 per-pair queries)
    // ========================================================================

    /**
     * Counts spots per band+mode in 15-minute time buckets.
     *
     * <p>Uses TimescaleDB's {@code time_bucket()} for efficient bucketing
     * aligned with hypertable chunk boundaries.
     *
     * <p>Returns rows of [band, mode, bucket_start, count] where bucket_start
     * is the start of each 15-minute interval. The caller aggregates these
     * fine-grained buckets into mode-appropriate windows in Java.
     *
     * <p>Replaces ~271 individual COUNT queries per aggregation run.
     *
     * @param since earliest time to include (should cover widest baseline window)
     * @return list of [band, mode, bucket_start, count] tuples
     */
    @Query(value = """
            SELECT band, mode,
                   time_bucket('15 minutes', spotted_at) AS bucket_start,
                   COUNT(*) AS cnt
            FROM spots
            WHERE spotted_at > :since
            GROUP BY band, mode, bucket_start
            ORDER BY band, mode, bucket_start
            """, nativeQuery = true)
    List<Object[]> countSpotsByBandModeInBuckets(@Param("since") Instant since);

    /**
     * Finds the max DX spot for each band+mode pair in one query.
     *
     * <p>Uses ROW_NUMBER() window function to find the spot with the highest
     * distance per band+mode pair, avoiding the N correlated subqueries that
     * previously took ~6 seconds each.
     *
     * <p>Replaces ~38 individual findMaxDxSpot queries per aggregation run.
     *
     * @param since minimum spotted_at time
     * @return list of [band, mode, distance_km, spotted_call, spotter_call] tuples
     */
    @Query(value = """
            SELECT band, mode, distance_km, spotted_call, spotter_call
            FROM (
              SELECT band, mode, distance_km, spotted_call, spotter_call,
                     ROW_NUMBER() OVER (
                       PARTITION BY band, mode
                       ORDER BY distance_km DESC NULLS LAST, spotted_at DESC
                     ) AS rn
              FROM spots
              WHERE spotted_at > :since AND distance_km IS NOT NULL
            ) ranked
            WHERE rn = 1
            """, nativeQuery = true)
    List<Object[]> findMaxDxSpotPerBandMode(@Param("since") Instant since);

    /**
     * Counts continent paths for all band+mode pairs in one query.
     *
     * <p>Replaces ~38 individual continent path queries per aggregation run.
     *
     * @param since minimum spotted_at time
     * @return list of [band, mode, spotter_continent, spotted_continent, count] tuples
     */
    @Query(value = """
            SELECT band, mode, spotter_continent, spotted_continent, COUNT(*) AS cnt
            FROM spots
            WHERE spotted_at > :since
              AND spotter_continent IS NOT NULL
              AND spotted_continent IS NOT NULL
              AND spotter_continent <> spotted_continent
            GROUP BY band, mode, spotter_continent, spotted_continent
            """, nativeQuery = true)
    List<Object[]> countContinentPathsPerBandMode(@Param("since") Instant since);

    // ========================================================================
    // Per-band+mode queries (used by single-pair aggregation path)
    // ========================================================================

    /**
     * Counts spots by band and mode within a time window.
     *
     * <p>Used for per-mode activity aggregation.
     *
     * @param band  the band (e.g., "20m")
     * @param mode  the mode (e.g., "FT4")
     * @param since minimum spotted_at time
     * @return count of spots on the band for the specific mode
     */
    long countByBandAndModeAndSpottedAtAfter(String band, String mode, Instant since);

    /**
     * Finds the spot with maximum distance for a specific band and mode.
     *
     * @param band  the band (e.g., "20m")
     * @param mode  the mode (e.g., "FT4")
     * @param since minimum spotted_at time
     * @return the spot with maximum distance, or empty if no spots
     */
    @Query("""
            SELECT s FROM SpotEntity s
            WHERE s.band = :band
            AND s.mode = :mode
            AND s.spottedAt > :since
            AND s.distanceKm = (
                SELECT MAX(s2.distanceKm) FROM SpotEntity s2
                WHERE s2.band = :band AND s2.mode = :mode AND s2.spottedAt > :since
            )
            ORDER BY s.spottedAt DESC
            LIMIT 1
            """)
    Optional<SpotEntity> findMaxDxSpotByBandAndModeAndSpottedAtAfter(
            @Param("band") String band,
            @Param("mode") String mode,
            @Param("since") Instant since);

    /**
     * Counts continent path occurrences for a band and mode within a time window.
     *
     * @param band  the band (e.g., "20m")
     * @param mode  the mode (e.g., "FT4")
     * @param since minimum spotted_at time
     * @return list of [spotterContinent, spottedContinent, count] tuples
     */
    @Query("""
            SELECT s.spotterContinent, s.spottedContinent, COUNT(s)
            FROM SpotEntity s
            WHERE s.band = :band
            AND s.mode = :mode
            AND s.spottedAt > :since
            AND s.spotterContinent IS NOT NULL
            AND s.spottedContinent IS NOT NULL
            AND s.spotterContinent <> s.spottedContinent
            GROUP BY s.spotterContinent, s.spottedContinent
            """)
    List<Object[]> countContinentPathsByBandAndModeAndSpottedAtAfter(
            @Param("band") String band,
            @Param("mode") String mode,
            @Param("since") Instant since);
}
