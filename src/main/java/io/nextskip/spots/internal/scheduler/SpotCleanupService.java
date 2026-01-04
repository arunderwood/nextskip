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
 * <p>The cleanup is transactional to ensure atomicity of the delete operation.
 */
@Service
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class SpotCleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(SpotCleanupService.class);

    private final SpotRepository spotRepository;
    private final Duration ttl;

    public SpotCleanupService(
            SpotRepository spotRepository,
            @Value("${nextskip.spots.persistence.ttl:24h}") Duration ttl) {
        this.spotRepository = spotRepository;
        this.ttl = ttl;
    }

    /**
     * Deletes spots older than the configured TTL.
     *
     * @return the number of spots deleted
     */
    @Transactional
    public int executeCleanup() {
        Instant cutoff = Instant.now().minus(ttl);
        LOG.debug("Cleaning up spots older than {}", cutoff);

        int deleted = spotRepository.deleteByCreatedAtBefore(cutoff);

        if (deleted > 0) {
            LOG.info("Spot cleanup complete: deleted {} spots older than {}", deleted, ttl);
        } else {
            LOG.debug("Spot cleanup complete: no expired spots found");
        }

        return deleted;
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
