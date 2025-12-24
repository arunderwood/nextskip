package io.nextskip.activations.api;

import io.nextskip.activations.model.Activation;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for activations data exposed to frontend.
 *
 * <p>Contains combined POTA and SOTA activation data for dashboard display.
 *
 * @param potaActivations List of current POTA activations
 * @param sotaActivations List of current SOTA activations
 * @param totalCount Total number of activations across both sources
 * @param lastUpdated Timestamp when this data was generated
 */
public record ActivationsResponse(
        List<Activation> potaActivations,
        List<Activation> sotaActivations,
        int totalCount,
        Instant lastUpdated
) {
    /**
     * Compact constructor for defensive copying of mutable collections.
     */
    public ActivationsResponse {
        potaActivations = potaActivations != null ? List.copyOf(potaActivations) : List.of();
        sotaActivations = sotaActivations != null ? List.copyOf(sotaActivations) : List.of();
    }
}
