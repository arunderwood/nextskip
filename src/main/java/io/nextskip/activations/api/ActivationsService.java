package io.nextskip.activations.api;

import io.nextskip.activations.model.ActivationsSummary;

/**
 * Public API for activations data (POTA/SOTA).
 *
 * <p>This is the module's public contract - other modules should depend on this
 * interface rather than the internal implementation.
 */
public interface ActivationsService {

    /**
     * Get current activations summary combining POTA and SOTA data.
     *
     * @return Summary of all current activations
     */
    ActivationsSummary getActivationsSummary();

    /**
     * Get activations data formatted for dashboard display.
     *
     * <p>Returns activations separated by type (POTA/SOTA) with metadata.
     * This is the primary method for dashboard presentation, providing a
     * complete response object ready for frontend consumption.
     *
     * @return ActivationsResponse with separated activation lists and metadata
     */
    ActivationsResponse getActivationsResponse();
}
