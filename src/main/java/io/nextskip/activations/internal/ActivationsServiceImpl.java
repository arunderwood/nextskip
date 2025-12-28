package io.nextskip.activations.internal;

import io.nextskip.activations.api.ActivationsResponse;
import io.nextskip.activations.api.ActivationsService;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.ActivationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ActivationsService.
 *
 * <p>Aggregates data from POTA and SOTA clients to provide a unified view
 * of current activations for the dashboard.
 */
@Service
public class ActivationsServiceImpl implements ActivationsService {

    private static final Logger LOG = LoggerFactory.getLogger(ActivationsServiceImpl.class);

    private final PotaClient potaClient;
    private final SotaClient sotaClient;

    @Autowired
    public ActivationsServiceImpl(PotaClient potaClient, SotaClient sotaClient) {
        this.potaClient = potaClient;
        this.sotaClient = sotaClient;
    }

    @Override
    public ActivationsSummary getActivationsSummary() {
        LOG.debug("Fetching activations summary");

        // Fetch from both sources concurrently (using parallel streams or ExecutorService in production)
        List<Activation> potaActivations = fetchPotaActivations();
        List<Activation> sotaActivations = fetchSotaActivations();

        // Combine activations
        List<Activation> allActivations = new ArrayList<>();
        allActivations.addAll(potaActivations);
        allActivations.addAll(sotaActivations);

        // Count by type
        int potaCount = potaActivations.size();
        int sotaCount = sotaActivations.size();

        LOG.info("Activations summary: {} POTA, {} SOTA (total: {})",
                potaCount, sotaCount, potaCount + sotaCount);

        return new ActivationsSummary(
                allActivations,
                potaCount,
                sotaCount,
                Instant.now()
        );
    }

    /**
     * Fetch POTA activations with graceful degradation.
     *
     * <p>Primary error handling via PotaClient's Resilience4j annotations.
     * Service-level fallback ensures dashboard remains functional even if
     * client-level resilience is bypassed (e.g., in unit tests or edge cases).
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private List<Activation> fetchPotaActivations() {
        try {
            return potaClient.fetch();
        } catch (RuntimeException e) {
            LOG.warn("Failed to fetch POTA activations: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetch SOTA activations with graceful degradation.
     *
     * <p>Primary error handling via SotaClient's Resilience4j annotations.
     * Service-level fallback ensures dashboard remains functional even if
     * client-level resilience is bypassed (e.g., in unit tests or edge cases).
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private List<Activation> fetchSotaActivations() {
        try {
            return sotaClient.fetch();
        } catch (RuntimeException e) {
            LOG.warn("Failed to fetch SOTA activations: {}", e.getMessage());
            return List.of();
        }
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
