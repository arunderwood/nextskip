package io.nextskip.meteors.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.EventStatus;
import io.nextskip.meteors.api.MeteorService;
import io.nextskip.meteors.model.MeteorShower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of MeteorService.
 *
 * <p>Reads meteor shower data from the LoadingCache backed by the database.
 * Cache is populated by MeteorRefreshTask which loads from the JSON data file,
 * saves to DB, then triggers async cache refresh.
 */
@Service
public class MeteorServiceImpl implements MeteorService {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorServiceImpl.class);

    private final LoadingCache<String, List<MeteorShower>> meteorShowersCache;

    public MeteorServiceImpl(LoadingCache<String, List<MeteorShower>> meteorShowersCache) {
        this.meteorShowersCache = meteorShowersCache;
    }

    @Override
    public List<MeteorShower> getMeteorShowers() {
        LOG.debug("Fetching meteor showers from cache");

        List<MeteorShower> showers = meteorShowersCache.get(CacheConfig.CACHE_KEY);
        if (showers == null) {
            showers = List.of();
        }

        // Filter out ended showers (cache contains all current showers from DB)
        List<MeteorShower> relevant = showers.stream()
                .filter(s -> s.getStatus() != EventStatus.ENDED)
                .toList();

        LOG.info("Retrieved {} relevant meteor showers from cache", relevant.size());
        return relevant;
    }

    @Override
    public List<MeteorShower> getActiveShowers() {
        return getMeteorShowers().stream()
                .filter(s -> s.getStatus() == EventStatus.ACTIVE)
                .toList();
    }

    @Override
    public List<MeteorShower> getUpcomingShowers() {
        return getMeteorShowers().stream()
                .filter(s -> s.getStatus() == EventStatus.UPCOMING)
                .toList();
    }
}
