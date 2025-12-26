package io.nextskip.meteors.internal;

import io.nextskip.common.client.RefreshableDataSource;
import io.nextskip.common.model.EventStatus;
import io.nextskip.meteors.api.MeteorService;
import io.nextskip.meteors.model.MeteorShower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of MeteorService.
 *
 * <p>Retrieves meteor shower data from the data loader and provides
 * filtered views for different use cases.
 */
@Service
public class MeteorServiceImpl implements MeteorService, RefreshableDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorServiceImpl.class);
    private static final int DEFAULT_LOOKAHEAD_DAYS = 30;
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final MeteorShowerDataLoader dataLoader;

    public MeteorServiceImpl(MeteorShowerDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @Override
    public String getSourceName() {
        return "Meteor Showers";
    }

    @Override
    public void refresh() {
        getMeteorShowers();
    }

    @Override
    public Duration getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    @Override
    @Cacheable(value = "meteorShowers", unless = "#result == null")
    public List<MeteorShower> getMeteorShowers() {
        LOG.debug("Fetching meteor showers");
        List<MeteorShower> showers = dataLoader.getShowers(DEFAULT_LOOKAHEAD_DAYS);

        // Filter out ended showers
        List<MeteorShower> relevant = showers.stream()
                .filter(s -> s.getStatus() != EventStatus.ENDED)
                .toList();

        LOG.info("Retrieved {} relevant meteor showers", relevant.size());
        return relevant;
    }

    @Override
    public Optional<MeteorShower> getPrimaryShower() {
        return getMeteorShowers().stream()
                .max(Comparator.comparingInt(MeteorShower::getScore));
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
