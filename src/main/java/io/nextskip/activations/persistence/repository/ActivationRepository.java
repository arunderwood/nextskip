package io.nextskip.activations.persistence.repository;

import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for activation persistence operations.
 */
@Repository
public interface ActivationRepository extends JpaRepository<ActivationEntity, Long> {

    /**
     * Find an activation by spot ID and source.
     *
     * @param spotId the unique spot identifier
     * @param source the data source
     * @return the activation if found
     */
    Optional<ActivationEntity> findBySpotIdAndSource(String spotId, String source);

    /**
     * Find all activations spotted after a given timestamp.
     *
     * @param spottedAt the timestamp to filter from
     * @return list of recent activations ordered by spotted time descending
     */
    List<ActivationEntity> findBySpottedAtAfterOrderBySpottedAtDesc(Instant spottedAt);

    /**
     * Find all activations of a specific type spotted after a given timestamp.
     *
     * @param type the activation type (POTA or SOTA)
     * @param spottedAt the timestamp to filter from
     * @return list of recent activations of the given type
     */
    List<ActivationEntity> findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
            ActivationType type, Instant spottedAt);

    /**
     * Find all activations by a specific activator.
     *
     * @param activatorCallsign the activator's callsign
     * @return list of activations by this operator
     */
    List<ActivationEntity> findByActivatorCallsignOrderBySpottedAtDesc(String activatorCallsign);

    /**
     * Find all activations at a specific location.
     *
     * @param locationReference the park/summit reference code
     * @return list of activations at this location
     */
    List<ActivationEntity> findByLocationReferenceOrderBySpottedAtDesc(String locationReference);

    /**
     * Find all activations from a specific data source.
     *
     * @param source the data source identifier
     * @return list of activations from this source
     */
    List<ActivationEntity> findBySourceOrderBySpottedAtDesc(String source);

    /**
     * Find activations from a specific source with spot IDs in the given list.
     *
     * <p>Used by refresh tasks for selective upsert - fetches only entities
     * that match incoming data, avoiding loading all historical records.
     *
     * @param source the data source identifier (e.g., "POTA API", "SOTA API")
     * @param spotIds list of spot IDs to look up
     * @return list of matching activations
     */
    List<ActivationEntity> findBySourceAndSpotIdIn(String source, List<String> spotIds);

    /**
     * Delete activations older than a given timestamp.
     *
     * @param spottedAt the cutoff timestamp
     * @return number of deleted records
     */
    int deleteBySpottedAtBefore(Instant spottedAt);

    /**
     * Delete activations from a specific source older than a given timestamp.
     *
     * <p>Use this method instead of {@link #deleteBySpottedAtBefore(Instant)} when
     * multiple refresh tasks run concurrently to avoid race conditions.
     *
     * @param source the data source identifier (e.g., "POTA API", "SOTA API")
     * @param spottedAt the cutoff timestamp
     * @return number of deleted records
     */
    int deleteBySourceAndSpottedAtBefore(String source, Instant spottedAt);
}
