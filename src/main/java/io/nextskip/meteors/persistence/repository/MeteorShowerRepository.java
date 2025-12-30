package io.nextskip.meteors.persistence.repository;

import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for meteor shower persistence operations.
 */
@Repository
public interface MeteorShowerRepository extends JpaRepository<MeteorShowerEntity, Long> {

    /**
     * Find a meteor shower by its unique code.
     *
     * @param code the shower code (e.g., "PER" for Perseids)
     * @return the shower, if found
     */
    Optional<MeteorShowerEntity> findByCode(String code);

    /**
     * Find all showers with visibility starting after a given timestamp.
     *
     * @param visibilityStart the timestamp to filter from
     * @return list of upcoming showers ordered by visibility start
     */
    List<MeteorShowerEntity> findByVisibilityStartAfterOrderByVisibilityStartAsc(Instant visibilityStart);

    /**
     * Find all showers currently active (visibility window contains now).
     *
     * @param now the current timestamp
     * @return list of active showers ordered by peak start
     */
    List<MeteorShowerEntity> findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
            Instant now, Instant alsoNow);

    /**
     * Find all showers with peak starting after a given timestamp.
     *
     * @param peakStart the timestamp to filter from
     * @return list of showers ordered by peak start
     */
    List<MeteorShowerEntity> findByPeakStartAfterOrderByPeakStartAsc(Instant peakStart);
}
