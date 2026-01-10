package io.nextskip.contests.persistence.repository;

import io.nextskip.contests.persistence.entity.ContestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for contest persistence operations.
 */
@Repository
public interface ContestRepository extends JpaRepository<ContestEntity, Long> {

    /**
     * Find all contests starting after a given timestamp.
     *
     * @param startTime the timestamp to filter from
     * @return list of upcoming contests ordered by start time
     */
    List<ContestEntity> findByStartTimeAfterOrderByStartTimeAsc(Instant startTime);

    /**
     * Find all contests that are currently active.
     *
     * @param now the current timestamp (passed twice for before/after comparison)
     * @return list of active contests ordered by end time
     */
    List<ContestEntity> findByStartTimeBeforeAndEndTimeAfterOrderByEndTimeAsc(
            Instant now, Instant alsoNow);

    /**
     * Find all contests ending after a given timestamp.
     *
     * @param endTime the timestamp to filter from
     * @return list of contests ordered by start time
     */
    List<ContestEntity> findByEndTimeAfterOrderByStartTimeAsc(Instant endTime);

    /**
     * Find all contests within a date range (overlapping).
     *
     * @param start range start
     * @param end   range end
     * @return list of contests with any overlap in the range
     */
    List<ContestEntity> findByStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
            Instant end, Instant start);

    /**
     * Find contests by sponsor.
     *
     * @param sponsor the sponsoring organization
     * @return list of contests by that sponsor
     */
    List<ContestEntity> findBySponsorOrderByStartTimeAsc(String sponsor);

    /**
     * Find all contests with a given WA7BNM reference.
     *
     * @param wa7bnmRef the WA7BNM reference identifier
     * @return list of contests with this reference
     */
    List<ContestEntity> findByWa7bnmRef(String wa7bnmRef);

    /**
     * Find all distinct WA7BNM references for contests ending after a given time.
     *
     * @param endTime the minimum end time
     * @return list of distinct references
     */
    @Query("SELECT DISTINCT c.wa7bnmRef FROM ContestEntity c WHERE c.endTime > :endTime AND c.wa7bnmRef IS NOT NULL")
    List<String> findDistinctWa7bnmRefsByEndTimeAfter(Instant endTime);
}
