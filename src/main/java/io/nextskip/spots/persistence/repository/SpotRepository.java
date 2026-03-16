package io.nextskip.spots.persistence.repository;

import io.nextskip.spots.persistence.entity.SpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PSKReporter spots.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Batch insert via inherited {@code saveAll()}</li>
 *   <li>TTL-based cleanup via {@code deleteByCreatedAtBefore()}</li>
 *   <li>Queries for Phase 2 API endpoints</li>
 * </ul>
 */
@Repository
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // JPQL parameter names are intentionally repeated
public interface SpotRepository extends JpaRepository<SpotEntity, Long> {

    /**
     * Deletes spots older than the specified cutoff time.
     *
     * <p>Used by the cleanup task for 24hr TTL expiration.
     *
     * @param cutoff delete spots created before this time
     * @return number of spots deleted
     * @deprecated Use {@link #deleteExpiredSpotsBatch(Instant, int)} for large-scale cleanup
     */
    @Deprecated
    int deleteByCreatedAtBefore(Instant cutoff);

    /**
     * Deletes a batch of expired spots using native SQL for efficiency.
     *
     * <p>Uses ctid-based deletion to avoid loading entities into memory.
     * This is critical for high-volume cleanup where millions of rows may
     * need to be deleted. Called repeatedly by cleanup service until no
     * more expired spots remain.
     *
     * @param cutoff delete spots created before this time
     * @param batchSize maximum rows to delete per call
     * @return number of spots deleted
     */
    @Modifying
    @Query(value = """
            DELETE FROM spots
            WHERE ctid IN (
                SELECT ctid FROM spots
                WHERE created_at < :cutoff
                LIMIT :batchSize
            )
            """, nativeQuery = true)
    int deleteExpiredSpotsBatch(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    /**
     * Finds recent spots by band, ordered by spotted time descending.
     *
     * @param band      the band (e.g., "20m", "40m")
     * @param spottedAt minimum spotted_at time
     * @return list of spots, most recent first
     */
    List<SpotEntity> findByBandAndSpottedAtAfterOrderBySpottedAtDesc(String band, Instant spottedAt);

    /**
     * Finds recent spots by mode, ordered by spotted time descending.
     *
     * @param mode      the mode (e.g., "FT8", "CW")
     * @param spottedAt minimum spotted_at time
     * @return list of spots, most recent first
     */
    List<SpotEntity> findByModeAndSpottedAtAfterOrderBySpottedAtDesc(String mode, Instant spottedAt);

    /**
     * Finds the most recent spot in the database.
     *
     * <p>Used for status/health reporting.
     *
     * @return the most recent spot, if any
     */
    Optional<SpotEntity> findTopByOrderBySpottedAtDesc();

    /**
     * Counts spots created after the specified time.
     *
     * <p>Useful for rate metrics.
     *
     * @param createdAt minimum created_at time
     * @return count of spots
     */
    long countByCreatedAtAfter(Instant createdAt);

    /**
     * Finds top DX spots by band, ordered by distance descending.
     *
     * @param band  the band (e.g., "20m")
     * @param limit maximum number of results
     * @return list of spots with highest distances
     */
    List<SpotEntity> findTopByBandAndDistanceKmNotNullOrderByDistanceKmDesc(String band);

    // ========================================================================
    // Phase 2: Band Activity Aggregation Queries
    // ========================================================================

    // --- Bulk aggregation queries (replace N+1 per-pair queries) ---

    /**
     * Counts spots per band+mode in 15-minute time buckets.
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
                   date_trunc('hour', spotted_at)
                     + INTERVAL '15 min' * FLOOR(EXTRACT(MINUTE FROM spotted_at) / 15) AS bucket_start,
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

    /**
     * Counts spots by band within a time window.
     *
     * <p>Used for current activity aggregation.
     *
     * @param band  the band (e.g., "20m")
     * @param since minimum spotted_at time
     * @return count of spots on the band since the given time
     */
    long countByBandAndSpottedAtAfter(String band, Instant since);

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
     * Finds the maximum distance for a band within a time window.
     *
     * @param band  the band (e.g., "20m")
     * @param since minimum spotted_at time
     * @return maximum distance in km, or empty if no spots with distance data
     */
    @Query("""
            SELECT MAX(s.distanceKm) FROM SpotEntity s
            WHERE s.band = :band
            AND s.spottedAt > :since
            AND s.distanceKm IS NOT NULL
            """)
    Optional<Integer> findMaxDistanceByBandAndSpottedAtAfter(
            @Param("band") String band,
            @Param("since") Instant since);

    /**
     * Finds the spot with maximum distance for DX path details.
     *
     * <p>Returns the most recent spot with the maximum distance on a band.
     * Used to provide callsign details for the max DX path display.
     *
     * @param band  the band (e.g., "20m")
     * @param since minimum spotted_at time
     * @return the spot with maximum distance, or empty if no spots
     */
    @Query("""
            SELECT s FROM SpotEntity s
            WHERE s.band = :band
            AND s.spottedAt > :since
            AND s.distanceKm = (
                SELECT MAX(s2.distanceKm) FROM SpotEntity s2
                WHERE s2.band = :band AND s2.spottedAt > :since
            )
            ORDER BY s.spottedAt DESC
            LIMIT 1
            """)
    Optional<SpotEntity> findMaxDxSpotByBandAndSpottedAtAfter(
            @Param("band") String band,
            @Param("since") Instant since);

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
     * Counts continent path occurrences for a band within a time window.
     *
     * <p>Returns rows of [spotterContinent, spottedContinent, count] for
     * cross-continent spots. Used to determine which major paths are active.
     *
     * @param band  the band (e.g., "20m")
     * @param since minimum spotted_at time
     * @return list of [spotterContinent, spottedContinent, count] tuples
     */
    @Query("""
            SELECT s.spotterContinent, s.spottedContinent, COUNT(s)
            FROM SpotEntity s
            WHERE s.band = :band
            AND s.spottedAt > :since
            AND s.spotterContinent IS NOT NULL
            AND s.spottedContinent IS NOT NULL
            AND s.spotterContinent <> s.spottedContinent
            GROUP BY s.spotterContinent, s.spottedContinent
            """)
    List<Object[]> countContinentPathsByBandAndSpottedAtAfter(
            @Param("band") String band,
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

    /**
     * Finds distinct bands with activity since the given time.
     *
     * <p>Used to determine which bands to aggregate.
     *
     * @param since minimum spotted_at time
     * @return list of band names with recent activity
     */
    @Query("SELECT DISTINCT s.band FROM SpotEntity s WHERE s.spottedAt > :since")
    List<String> findDistinctBandsWithActivitySince(@Param("since") Instant since);

    /**
     * Finds distinct band+mode combinations with activity since the given time.
     *
     * <p>Returns rows of [band, mode] for all active band+mode pairs.
     * Used by the multi-mode aggregator to determine which combinations to aggregate.
     *
     * @param since minimum spotted_at time
     * @return list of [band, mode] tuples
     */
    @Query("""
            SELECT DISTINCT s.band, s.mode
            FROM SpotEntity s
            WHERE s.spottedAt > :since
            """)
    List<Object[]> findDistinctBandModePairsWithActivitySince(@Param("since") Instant since);

    /**
     * Finds mode distribution for a band to determine primary mode.
     *
     * <p>Returns rows of [mode, count] ordered by count descending.
     * The first result is the most common mode on the band.
     *
     * @param band  the band (e.g., "20m")
     * @param since minimum spotted_at time
     * @return list of [mode, count] tuples, most common first
     */
    @Query("""
            SELECT s.mode, COUNT(s) as cnt
            FROM SpotEntity s
            WHERE s.band = :band
            AND s.spottedAt > :since
            GROUP BY s.mode
            ORDER BY cnt DESC
            """)
    List<Object[]> findModeDistributionByBandSince(
            @Param("band") String band,
            @Param("since") Instant since);
}
