package io.nextskip.activations.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.activations.api.ActivationsResponse;
import io.nextskip.activations.api.ActivationsService;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.common.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of ActivationsService.
 *
 * <p>Reads activation data from the LoadingCache backed by the database.
 * Cache is populated by PotaRefreshTask and SotaRefreshTask which fetch
 * from external APIs, save to DB, then trigger async cache refresh.
 */
@Service
public class ActivationsServiceImpl implements ActivationsService {

    private static final Logger LOG = LoggerFactory.getLogger(ActivationsServiceImpl.class);

    private final LoadingCache<String, List<Activation>> activationsCache;

    @Autowired
    public ActivationsServiceImpl(LoadingCache<String, List<Activation>> activationsCache) {
        this.activationsCache = activationsCache;
    }

    @Override
    public ActivationsSummary getActivationsSummary() {
        LOG.debug("Fetching activations summary from cache");

        List<Activation> allActivations = activationsCache.get(CacheConfig.CACHE_KEY);
        if (allActivations == null) {
            allActivations = List.of();
        }

        // Count by type
        int potaCount = (int) allActivations.stream()
                .filter(a -> a.type() == ActivationType.POTA)
                .count();
        int sotaCount = (int) allActivations.stream()
                .filter(a -> a.type() == ActivationType.SOTA)
                .count();

        LOG.info("Activations summary: {} POTA, {} SOTA (total: {})",
                potaCount, sotaCount, potaCount + sotaCount);

        return new ActivationsSummary(
                allActivations,
                potaCount,
                sotaCount,
                Instant.now()
        );
    }

    @Override
    public ActivationsResponse getActivationsResponse() {
        LOG.debug("Building activations response for dashboard");

        ActivationsSummary summary = getActivationsSummary();

        // Separate activations by type (business logic in service layer)
        List<Activation> potaActivations = summary.activations().stream()
                .filter(a -> a.type() == ActivationType.POTA)
                .toList();

        List<Activation> sotaActivations = summary.activations().stream()
                .filter(a -> a.type() == ActivationType.SOTA)
                .toList();

        int totalCount = potaActivations.size() + sotaActivations.size();

        ActivationsResponse response = new ActivationsResponse(
                potaActivations,
                sotaActivations,
                totalCount,
                summary.lastUpdated()
        );

        LOG.debug("Returning activations response: {} POTA, {} SOTA (total: {})",
                potaActivations.size(), sotaActivations.size(), totalCount);

        return response;
    }
}
