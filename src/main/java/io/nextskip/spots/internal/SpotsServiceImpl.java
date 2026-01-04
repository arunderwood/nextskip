package io.nextskip.spots.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.spots.api.BandActivityResponse;
import io.nextskip.spots.api.SpotsService;
import io.nextskip.spots.internal.client.SpotSource;
import io.nextskip.spots.internal.stream.SpotStreamProcessor;
import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link SpotsService}.
 *
 * <p>Provides spot status, statistics, and band activity data through
 * a combination of direct repository access and cached aggregations.
 */
@Service
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class SpotsServiceImpl implements SpotsService {

    private static final Logger LOG = LoggerFactory.getLogger(SpotsServiceImpl.class);

    private final SpotSource spotSource;
    private final SpotRepository spotRepository;
    private final SpotStreamProcessor streamProcessor;
    private final LoadingCache<String, Map<String, BandActivity>> bandActivityCache;
    private final Clock clock;

    /**
     * Constructs a SpotsServiceImpl with all dependencies.
     *
     * @param spotSource the spot source for connection status
     * @param spotRepository the repository for spot data
     * @param streamProcessor the stream processor for spot statistics
     * @param bandActivityCache the cache for band activity aggregations (optional, may be null)
     * @param clock the clock for time-based operations
     */
    public SpotsServiceImpl(
            SpotSource spotSource,
            SpotRepository spotRepository,
            SpotStreamProcessor streamProcessor,
            @org.springframework.lang.Nullable LoadingCache<String, Map<String, BandActivity>> bandActivityCache,
            Clock clock) {
        this.spotSource = spotSource;
        this.spotRepository = spotRepository;
        this.streamProcessor = streamProcessor;
        this.bandActivityCache = bandActivityCache;
        this.clock = clock;
    }

    // ========================================================================
    // Phase 1: Status and Statistics
    // ========================================================================

    @Override
    public boolean isConnected() {
        return spotSource.isConnected();
    }

    @Override
    public String getSourceName() {
        return spotSource.getSourceName();
    }

    @Override
    public long getSpotCount() {
        return spotRepository.count();
    }

    @Override
    public Optional<Instant> getLastSpotTime() {
        return spotRepository.findTopByOrderBySpottedAtDesc()
                .map(entity -> entity.getSpottedAt());
    }

    @Override
    public long getSpotsProcessed() {
        return streamProcessor.getSpotsProcessed();
    }

    @Override
    public long getBatchesPersisted() {
        return streamProcessor.getBatchesPersisted();
    }

    @Override
    public long getSpotCountSince(int minutes) {
        Instant cutoff = clock.instant().minusSeconds(minutes * 60L);
        return spotRepository.countByCreatedAtAfter(cutoff);
    }

    // ========================================================================
    // Phase 2: Band Activity Aggregation
    // ========================================================================

    @Override
    public Map<String, BandActivity> getCurrentActivity() {
        LOG.debug("Fetching current band activity from cache");
        if (bandActivityCache == null) {
            LOG.warn("Band activity cache not available - aggregation feature disabled");
            return Map.of();
        }
        Map<String, BandActivity> activity = bandActivityCache.get(CacheConfig.CACHE_KEY);
        return activity != null ? activity : Map.of();
    }

    @Override
    public Optional<BandActivity> getBandActivity(String band) {
        LOG.debug("Fetching activity for band: {}", band);
        return Optional.ofNullable(getCurrentActivity().get(band));
    }

    @Override
    public BandActivityResponse getBandActivityResponse() {
        LOG.debug("Building band activity response for dashboard");
        Map<String, BandActivity> activities = getCurrentActivity();
        return new BandActivityResponse(
                activities,
                clock.instant(),
                isConnected()
        );
    }

    @Override
    public List<Spot> getRecentSpots(String band, Duration window) {
        LOG.debug("Fetching recent spots for band {} with window {}", band, window);
        Instant since = clock.instant().minus(window);
        return spotRepository.findByBandAndSpottedAtAfterOrderBySpottedAtDesc(band, since)
                .stream()
                .map(entity -> entity.toDomain())
                .toList();
    }
}
