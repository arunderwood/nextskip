package io.nextskip.propagation.persistence.repository;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for band condition persistence operations.
 */
@Repository
public interface BandConditionRepository extends JpaRepository<BandConditionEntity, Long> {

    /**
     * Find the most recent condition for a specific band.
     *
     * @param band the frequency band
     * @return the most recent condition, if any
     */
    Optional<BandConditionEntity> findTopByBandOrderByRecordedAtDesc(FrequencyBand band);

    /**
     * Find all conditions for a specific band after a given timestamp.
     *
     * @param band  the frequency band
     * @param since the timestamp to filter from
     * @return list of conditions ordered by most recent first
     */
    List<BandConditionEntity> findByBandAndRecordedAtAfterOrderByRecordedAtDesc(
            FrequencyBand band, Instant since);

    /**
     * Find the most recent condition for each band (deduplicated).
     *
     * <p>Uses PostgreSQL's DISTINCT ON to return exactly one record per band,
     * selecting the most recently recorded condition for each.
     *
     * @param since only consider conditions after this timestamp
     * @return list with one condition per band, the most recent for each
     */
    @Query(
            value =
                    """
            SELECT DISTINCT ON (band) *
            FROM band_conditions
            WHERE recorded_at > :since
            ORDER BY band, recorded_at DESC
            """,
            nativeQuery = true)
    List<BandConditionEntity> findLatestPerBandSince(@Param("since") Instant since);

    /**
     * Find all conditions recorded after a given timestamp.
     *
     * <p>Note: This returns ALL records, not deduplicated by band.
     * For deduplicated results, use {@link #findLatestPerBandSince(Instant)}.
     *
     * @param since the timestamp to filter from
     * @return list of all conditions ordered by most recent first
     */
    List<BandConditionEntity> findByRecordedAtAfterOrderByRecordedAtDesc(Instant since);

    /**
     * Find all conditions recorded after a given timestamp.
     *
     * @param since the timestamp to filter from
     * @return list of conditions ordered by band then timestamp
     */
    List<BandConditionEntity> findByRecordedAtAfterOrderByBandAscRecordedAtDesc(Instant since);
}
