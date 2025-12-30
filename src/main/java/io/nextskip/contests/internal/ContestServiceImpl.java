package io.nextskip.contests.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.EventStatus;
import io.nextskip.contests.api.ContestService;
import io.nextskip.contests.api.ContestsResponse;
import io.nextskip.contests.model.Contest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Implementation of ContestService.
 *
 * <p>Reads contest data from the LoadingCache backed by the database.
 * Cache is populated by ContestRefreshTask which fetches from the
 * WA7BNM iCal feed, saves to DB, then triggers async cache refresh.
 */
@Service
public class ContestServiceImpl implements ContestService {

    private static final Logger LOG = LoggerFactory.getLogger(ContestServiceImpl.class);

    private final LoadingCache<String, List<Contest>> contestsCache;

    @Autowired
    public ContestServiceImpl(LoadingCache<String, List<Contest>> contestsCache) {
        this.contestsCache = contestsCache;
    }

    @Override
    public List<Contest> getUpcomingContests() {
        LOG.debug("Fetching upcoming contests from cache");

        List<Contest> contests = contestsCache.get(CacheConfig.CACHE_KEY);
        if (contests == null) {
            contests = List.of();
        }

        LOG.info("Retrieved {} upcoming contests from cache", contests.size());
        return contests;
    }

    @Override
    public ContestsResponse getContestsResponse() {
        LOG.debug("Building contests response for dashboard");

        List<Contest> contests = getUpcomingContests();

        // Calculate counts by status (business logic in service layer)
        int activeCount = (int) contests.stream()
                .filter(c -> c.getStatus() == EventStatus.ACTIVE)
                .count();

        int upcomingCount = (int) contests.stream()
                .filter(c -> c.getStatus() == EventStatus.UPCOMING)
                .filter(c -> c.getTimeRemaining().compareTo(Duration.ofHours(24)) <= 0)
                .count();

        int totalCount = contests.size();

        ContestsResponse response = new ContestsResponse(
                contests,
                activeCount,
                upcomingCount,
                totalCount,
                Instant.now()
        );

        LOG.debug("Returning contests response: {} active, {} upcoming soon, {} total",
                activeCount, upcomingCount, totalCount);

        return response;
    }
}
