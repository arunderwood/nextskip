package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.propagation.internal.HamQslClient;
import io.nextskip.propagation.internal.HamQslFetchResult;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Unified service for refreshing HamQSL solar and band condition data.
 *
 * <p>This service consolidates the functionality of the previous separate services
 * ({@code HamQslSolarRefreshService} and {@code HamQslBandRefreshService}) to
 * eliminate duplicate HTTP fetches. A single call to the HamQSL API now provides
 * both solar indices and band conditions.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
public class HamQslRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(HamQslRefreshService.class);
    private static final String SERVICE_NAME = "HamQSL";

    private final HamQslClient hamQslClient;
    private final SolarIndicesRepository solarRepository;
    private final BandConditionRepository bandRepository;
    private final LoadingCache<String, SolarIndices> solarIndicesCache;
    private final LoadingCache<String, List<BandCondition>> bandConditionsCache;

    // Metrics for success message
    private Integer kIndex;
    private Integer aIndex;
    private int bandCount;
    private boolean skipped;

    public HamQslRefreshService(
            ApplicationEventPublisher eventPublisher,
            HamQslClient hamQslClient,
            SolarIndicesRepository solarRepository,
            BandConditionRepository bandRepository,
            LoadingCache<String, SolarIndices> solarIndicesCache,
            LoadingCache<String, List<BandCondition>> bandConditionsCache) {
        super(eventPublisher);
        this.hamQslClient = hamQslClient;
        this.solarRepository = solarRepository;
        this.bandRepository = bandRepository;
        this.solarIndicesCache = solarIndicesCache;
        this.bandConditionsCache = bandConditionsCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Fetch all data from API in a single HTTP request
        HamQslFetchResult result = hamQslClient.fetch();

        if (result == null) {
            LOG.warn("HamQSL client returned null - skipping save");
            this.skipped = true;
            return;
        }

        this.skipped = false;

        try {
            // Save solar indices
            SolarIndices solarIndices = result.solarIndices();
            if (solarIndices != null) {
                this.kIndex = solarIndices.kIndex();
                this.aIndex = solarIndices.aIndex();
                SolarIndicesEntity solarEntity = SolarIndicesEntity.fromDomain(solarIndices);
                solarRepository.save(solarEntity);
            }

            // Save band conditions
            List<BandCondition> bandConditions = result.bandConditions();
            if (bandConditions != null && !bandConditions.isEmpty()) {
                Instant now = Instant.now();
                List<BandConditionEntity> bandEntities = bandConditions.stream()
                        .map(bc -> BandConditionEntity.fromDomain(bc, now))
                        .toList();
                bandRepository.saveAll(bandEntities);
                this.bandCount = bandEntities.size();
            } else {
                this.bandCount = 0;
            }

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during HamQSL refresh", e);
        }
    }

    @Override
    protected CacheRefreshEvent createCacheRefreshEvent() {
        if (skipped) {
            return new CacheRefreshEvent("solarIndices+bandConditions (skipped)", () -> { });
        }
        // Refresh both caches
        return new CacheRefreshEvent("solarIndices+bandConditions", () -> {
            solarIndicesCache.refresh(CacheConfig.CACHE_KEY);
            bandConditionsCache.refresh(CacheConfig.CACHE_KEY);
        });
    }

    @Override
    protected String getSuccessMessage() {
        if (skipped) {
            return "HamQSL refresh skipped: null response from client";
        }
        return String.format("HamQSL refresh complete: K=%d, A=%d, %d band conditions",
                kIndex, aIndex, bandCount);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }
}
