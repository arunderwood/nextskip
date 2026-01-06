package io.nextskip.spots.internal.scheduler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for cleaning up expired spots from the database.
 *
 * <p>Spots are retained for a configurable TTL (default 24 hours).
 * This service is called by {@link SpotCleanupTask} on a recurring schedule.
 *
 * <p>Cleanup uses batched deletion to handle high-volume scenarios where
 * millions of rows may need to be deleted. Each batch is committed independently
 * to avoid long-running transactions and ensure progress is preserved.
 */
@Service
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class SpotCleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(SpotCleanupService.class);

    /**
     * Number of rows to delete per batch. Tuned to balance:
     * - Transaction size (not too large to avoid timeouts)
     * - Efficiency (not too small to avoid excessive round-trips)
     */
    private static final int CLEANUP_BATCH_SIZE = 100_000;

    private final SpotRepository spotRepository;
    private final Duration ttl;

    public SpotCleanupService(
            SpotRepository spotRepository,
            @Value("${nextskip.spots.persistence.ttl:24h}") Duration ttl) {
        this.spotRepository = spotRepository;
        this.ttl = ttl;
    }

    /**
     * Deletes spots older than the configured TTL using batched deletion.
     *
     * <p>Iteratively deletes batches of expired spots until none remain.
     * Each batch is committed independently to avoid long-running transactions.
     *
     * @return the total number of spots deleted
     */
    public int executeCleanup() {
        Instant cutoff = Instant.now().minus(ttl);
        LOG.debug("Cleaning up spots older than {}", cutoff);

        int totalDeleted = 0;
        int deleted;

        do {
            deleted = deleteBatch(cutoff);
            totalDeleted += deleted;
            if (deleted > 0) {
                LOG.debug("Deleted batch of {} spots (total so far: {})", deleted, totalDeleted);
            }
        } while (deleted == CLEANUP_BATCH_SIZE);

        if (totalDeleted > 0) {
            LOG.info("Spot cleanup complete: deleted {} spots older than {}", totalDeleted, ttl);
        } else {
            LOG.debug("Spot cleanup complete: no expired spots found");
        }

        return totalDeleted;
    }

    /**
     * Deletes a single batch of expired spots within its own transaction.
     *
     * @param cutoff delete spots created before this time
     * @return number of spots deleted in this batch
     */
    @Transactional
    int deleteBatch(Instant cutoff) {
        return spotRepository.deleteExpiredSpotsBatch(cutoff, CLEANUP_BATCH_SIZE);
    }

    /**
     * Returns the configured TTL for spots.
     *
     * @return the TTL duration
     */
    public Duration getTtl() {
        return ttl;
    }
}
