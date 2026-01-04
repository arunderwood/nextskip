package io.nextskip.spots.internal.enrichment;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.common.model.Callsign;
import io.nextskip.spots.model.Spot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Validates callsigns in spots and records failures for analysis.
 *
 * <p>This enricher implements permissive validation: spots with invalid
 * callsigns are kept (not dropped) but validation failures are sampled
 * and logged for bug analysis.
 *
 * <p>Both the spotter and spotted callsigns are validated. The source
 * name from the spot is used to identify the data source in failure records.
 *
 * <p>Integration in the pipeline:
 * <pre>{@code
 * queuePair.second()
 *     .map(parser::parse)
 *     .filter(Optional::isPresent)
 *     .map(Optional::get)
 *     .map(callsignEnricher::enrich)  // Validate callsigns
 *     .map(distanceEnricher::enrich)
 *     .map(continentEnricher::enrich)
 *     ...
 * }</pre>
 */
@Component
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class CallsignEnricher implements SpotEnricher {

    private final CallsignValidationSampler sampler;

    /**
     * Creates a new CallsignEnricher.
     *
     * @param sampler the sampler for recording validation failures
     */
    public CallsignEnricher(CallsignValidationSampler sampler) {
        this.sampler = sampler;
    }

    /**
     * Validates callsigns in the spot and records any failures.
     *
     * <p>Both spotterCall and spottedCall are validated. Invalid callsigns
     * are logged via the {@link CallsignValidationSampler} but the spot
     * is always returned (permissive validation).
     *
     * @param spot the spot to validate
     * @return the original spot (unchanged)
     */
    @Override
    public Spot enrich(Spot spot) {
        if (spot == null) {
            return null;
        }

        String source = spot.source() != null ? spot.source() : "unknown";

        // Validate spotter callsign
        validateCallsign(spot.spotterCall(), source + ":spotter");

        // Validate spotted callsign
        validateCallsign(spot.spottedCall(), source + ":spotted");

        // Always return the spot (permissive validation)
        return spot;
    }

    private void validateCallsign(String rawCallsign, String source) {
        if (rawCallsign == null || rawCallsign.isBlank()) {
            return; // Missing callsigns are handled elsewhere
        }

        try {
            Callsign callsign = new Callsign(rawCallsign);
            Callsign.ValidationResult result = callsign.validate();

            if (!result.isValid()) {
                sampler.recordFailure(rawCallsign, result.failure(), source);
            }
        } catch (IllegalArgumentException e) {
            // Callsign constructor rejection (null/blank) - shouldn't happen given the check above
            // but log it as TOO_SHORT for tracking purposes
            sampler.recordFailure(rawCallsign, Callsign.ValidationFailure.TOO_SHORT, source);
        }
    }
}
