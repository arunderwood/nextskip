package io.nextskip.spots.persistence.repository;

import io.nextskip.spots.persistence.entity.SpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
public interface SpotRepository extends JpaRepository<SpotEntity, Long> {

    /**
     * Deletes spots older than the specified cutoff time.
     *
     * <p>Used by the cleanup task for 24hr TTL expiration.
     *
     * @param cutoff delete spots created before this time
     * @return number of spots deleted
     */
    int deleteByCreatedAtBefore(Instant cutoff);

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
}
