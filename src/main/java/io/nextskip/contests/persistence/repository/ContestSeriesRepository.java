package io.nextskip.contests.persistence.repository;

import io.nextskip.contests.persistence.entity.ContestSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for contest series persistence operations.
 *
 * <p>Provides data access for WA7BNM contest series metadata. Each series
 * is uniquely identified by its WA7BNM reference (the {@code ref} parameter
 * from contest detail page URLs).
 */
@Repository
public interface ContestSeriesRepository extends JpaRepository<ContestSeriesEntity, Long> {

    /**
     * Find a contest series by its WA7BNM reference.
     *
     * @param wa7bnmRef the unique WA7BNM reference identifier
     * @return the contest series if found
     */
    Optional<ContestSeriesEntity> findByWa7bnmRef(String wa7bnmRef);
}
