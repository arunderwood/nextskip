package io.nextskip.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cache configuration using Caffeine LoadingCache.
 *
 * <p>Provides read-through caches backed by database repositories. Each cache
 * automatically loads from the database on cache miss. Schedulers trigger
 * {@code cache.refresh("all")} after writing to the database, ensuring
 * requests are always served from cache without blocking.
 *
 * <p>Cache TTLs serve as safety nets - normal cache updates happen via
 * scheduler-triggered refresh, not expiration.
 *
 * <p>Data flow:
 * <pre>
 * External API → db-scheduler task → Database → cache.refresh("all")
 *                                                      ↓
 * Browser ← @BrowserCallable ← Service ← LoadingCache ← DB (CacheLoader)
 * </pre>
 */
@Configuration
public class CacheConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConfig.class);

    private final Clock clock;

    public CacheConfig(Clock clock) {
        this.clock = clock;
    }

    /**
     * Single cache key used for all caches.
     * Each cache stores one list of domain objects.
     */
    public static final String CACHE_KEY = "all";

    // Safety net TTLs - caches are refreshed by schedulers, not expiration
    private static final Duration ACTIVATIONS_EXPIRY = Duration.ofMinutes(10);
    private static final Duration SOLAR_INDICES_EXPIRY = Duration.ofMinutes(15);
    private static final Duration BAND_CONDITIONS_EXPIRY = Duration.ofMinutes(45);
    private static final Duration CONTESTS_EXPIRY = Duration.ofHours(12);
    private static final Duration METEOR_SHOWERS_EXPIRY = Duration.ofHours(4);

    // Data retention periods for DB queries
    private static final Duration ACTIVATIONS_RETENTION = Duration.ofHours(2);
    private static final Duration BAND_CONDITIONS_RETENTION = Duration.ofHours(1);

    /**
     * LoadingCache for activations (POTA and SOTA combined).
     *
     * <p>Loads recent activations from the database. Both PotaRefreshTask and
     * SotaRefreshTask trigger refresh after saving their data.
     *
     * @param repository the activation repository
     * @return LoadingCache for activations
     */
    @Bean
    public LoadingCache<String, List<Activation>> activationsCache(ActivationRepository repository) {
        LOG.info("Creating activations LoadingCache with {} expiry", ACTIVATIONS_EXPIRY);
        return Caffeine.newBuilder()
                .expireAfterWrite(ACTIVATIONS_EXPIRY)
                .recordStats()
                .build(key -> loadActivations(repository));
    }

    /**
     * Loads activations from the database.
     * Package-private for testing.
     */
    List<Activation> loadActivations(ActivationRepository repository) {
        LOG.debug("Loading activations from database");
        Instant cutoff = Instant.now(clock).minus(ACTIVATIONS_RETENTION);
        List<Activation> activations = repository
                .findBySpottedAtAfterOrderBySpottedAtDesc(cutoff)
                .stream()
                .map(ActivationEntity::toDomain)
                .toList();
        LOG.info("Loaded {} activations from database", activations.size());
        return activations;
    }

    /**
     * LoadingCache for solar indices (merged NOAA + HamQSL data).
     *
     * <p>Loads and merges solar indices from both data sources. Prefers NOAA
     * for SFI/sunspots, HamQSL for K/A indices.
     *
     * @param repository the solar indices repository
     * @return LoadingCache for solar indices
     */
    @Bean
    public LoadingCache<String, SolarIndices> solarIndicesCache(SolarIndicesRepository repository) {
        LOG.info("Creating solarIndices LoadingCache with {} expiry", SOLAR_INDICES_EXPIRY);
        return Caffeine.newBuilder()
                .expireAfterWrite(SOLAR_INDICES_EXPIRY)
                .recordStats()
                .build(key -> {
                    LOG.debug("Loading solar indices from database");
                    return loadAndMergeSolarIndices(repository);
                });
    }

    /**
     * Loads and merges solar indices from NOAA and HamQSL sources.
     * Package-private for testing.
     */
    SolarIndices loadAndMergeSolarIndices(SolarIndicesRepository repository) {
        Optional<SolarIndicesEntity> noaaOpt = repository.findTopBySourceOrderByTimestampDesc("NOAA SWPC");
        Optional<SolarIndicesEntity> hamqslOpt = repository.findTopBySourceOrderByTimestampDesc("HamQSL");

        SolarIndices noaaData = noaaOpt.map(SolarIndicesEntity::toDomain).orElse(null);
        SolarIndices hamqslData = hamqslOpt.map(SolarIndicesEntity::toDomain).orElse(null);

        if (noaaData != null && hamqslData != null) {
            // Merge: NOAA for SFI/sunspots, HamQSL for K/A index
            SolarIndices merged = new SolarIndices(
                    noaaData.solarFluxIndex(),
                    hamqslData.aIndex(),
                    hamqslData.kIndex(),
                    noaaData.sunspotNumber(),
                    Instant.now(clock),
                    "NOAA SWPC + HamQSL"
            );
            LOG.info("Merged solar indices from NOAA and HamQSL");
            return merged;
        } else if (noaaData != null) {
            LOG.warn("HamQSL data unavailable, using NOAA data only");
            return noaaData;
        } else if (hamqslData != null) {
            LOG.warn("NOAA data unavailable, using HamQSL data only");
            return hamqslData;
        } else {
            LOG.error("No solar indices data available in database");
            return null;
        }
    }

    /**
     * LoadingCache for band conditions.
     *
     * <p>Loads recent band conditions from the database.
     *
     * @param repository the band condition repository
     * @return LoadingCache for band conditions
     */
    @Bean
    public LoadingCache<String, List<BandCondition>> bandConditionsCache(BandConditionRepository repository) {
        LOG.info("Creating bandConditions LoadingCache with {} expiry", BAND_CONDITIONS_EXPIRY);
        return Caffeine.newBuilder()
                .expireAfterWrite(BAND_CONDITIONS_EXPIRY)
                .recordStats()
                .build(key -> loadBandConditions(repository));
    }

    /**
     * Loads band conditions from the database.
     * Package-private for testing.
     */
    List<BandCondition> loadBandConditions(BandConditionRepository repository) {
        LOG.debug("Loading band conditions from database");
        Instant cutoff = Instant.now(clock).minus(BAND_CONDITIONS_RETENTION);
        List<BandCondition> conditions = repository
                .findLatestPerBandSince(cutoff)
                .stream()
                .map(BandConditionEntity::toDomain)
                .toList();
        LOG.info("Loaded {} band conditions from database", conditions.size());
        return conditions;
    }

    /**
     * LoadingCache for contests.
     *
     * <p>Loads upcoming contests from the database.
     *
     * @param repository the contest repository
     * @return LoadingCache for contests
     */
    @Bean
    public LoadingCache<String, List<Contest>> contestsCache(ContestRepository repository) {
        LOG.info("Creating contests LoadingCache with {} expiry", CONTESTS_EXPIRY);
        return Caffeine.newBuilder()
                .expireAfterWrite(CONTESTS_EXPIRY)
                .recordStats()
                .build(key -> loadContests(repository));
    }

    /**
     * Loads contests from the database.
     * Package-private for testing.
     */
    List<Contest> loadContests(ContestRepository repository) {
        LOG.debug("Loading contests from database");
        // Load contests ending after now (active + upcoming)
        List<Contest> contests = repository
                .findByEndTimeAfterOrderByStartTimeAsc(Instant.now(clock))
                .stream()
                .map(ContestEntity::toDomain)
                .toList();
        LOG.info("Loaded {} contests from database", contests.size());
        return contests;
    }

    /**
     * LoadingCache for meteor showers.
     *
     * <p>Loads active and upcoming meteor showers from the database.
     *
     * @param repository the meteor shower repository
     * @return LoadingCache for meteor showers
     */
    @Bean
    public LoadingCache<String, List<MeteorShower>> meteorShowersCache(MeteorShowerRepository repository) {
        LOG.info("Creating meteorShowers LoadingCache with {} expiry", METEOR_SHOWERS_EXPIRY);
        return Caffeine.newBuilder()
                .expireAfterWrite(METEOR_SHOWERS_EXPIRY)
                .recordStats()
                .build(key -> loadMeteorShowers(repository));
    }

    /**
     * Loads meteor showers from the database.
     * Package-private for testing.
     */
    List<MeteorShower> loadMeteorShowers(MeteorShowerRepository repository) {
        LOG.debug("Loading meteor showers from database");
        Instant now = Instant.now(clock);
        // Load showers where visibility window hasn't ended
        List<MeteorShower> showers = repository
                .findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(now, now)
                .stream()
                .map(MeteorShowerEntity::toDomain)
                .toList();
        LOG.info("Loaded {} meteor showers from database", showers.size());
        return showers;
    }
}
