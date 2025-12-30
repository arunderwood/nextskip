package io.nextskip.propagation.persistence.repository;

import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for solar indices persistence operations.
 */
@Repository
public interface SolarIndicesRepository extends JpaRepository<SolarIndicesEntity, Long> {

    /**
     * Find the most recent solar indices from a specific source.
     *
     * @param source the data source identifier
     * @return the most recent entry, if any
     */
    Optional<SolarIndicesEntity> findTopBySourceOrderByTimestampDesc(String source);

    /**
     * Find all solar indices after a given timestamp, ordered by most recent first.
     *
     * @param since the timestamp to filter from
     * @return list of entries after the given timestamp
     */
    List<SolarIndicesEntity> findByTimestampAfterOrderByTimestampDesc(Instant since);

    /**
     * Find the most recent solar indices regardless of source.
     *
     * @return the most recent entry, if any
     */
    Optional<SolarIndicesEntity> findTopByOrderByTimestampDesc();
}
