package io.nextskip.spots.internal.enrichment;

import io.nextskip.common.model.Callsign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate-limited sampler for callsign validation failures.
 *
 * <p>Samples a configurable fraction of validation failures to avoid log flooding
 * while still collecting enough data for bug analysis. Uses a sliding window buffer
 * to retain recent failures for inspection.
 *
 * <p>Thread-safe for use in the Pekko Streams pipeline.
 *
 * <p>Example configuration:
 * <pre>
 * nextskip.spots.validation.sample-rate=100  # Sample 1 in 100 failures
 * nextskip.spots.validation.buffer-size=1000 # Keep last 1000 sampled failures
 * </pre>
 */
@Component
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CallsignValidationSampler {

    private static final Logger LOG = LoggerFactory.getLogger(CallsignValidationSampler.class);

    private final int sampleRate;
    private final int bufferSize;

    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong sampledFailures = new AtomicLong(0);
    private final Deque<ValidationFailureRecord> recentFailures;

    /**
     * Record of a sampled validation failure.
     *
     * @param rawCallsign the original callsign string
     * @param failure the validation failure reason
     * @param source the data source (e.g., "PSKReporter")
     * @param timestamp when the failure occurred
     */
    public record ValidationFailureRecord(
            String rawCallsign,
            Callsign.ValidationFailure failure,
            String source,
            Instant timestamp
    ) { }

    /**
     * Creates a new CallsignValidationSampler.
     *
     * @param sampleRate sample 1 in N failures (default 100)
     * @param bufferSize maximum recent failures to retain (default 1000)
     */
    public CallsignValidationSampler(
            @Value("${nextskip.spots.validation.sample-rate:100}") int sampleRate,
            @Value("${nextskip.spots.validation.buffer-size:1000}") int bufferSize) {
        this.sampleRate = Math.max(1, sampleRate);
        this.bufferSize = Math.max(100, bufferSize);
        this.recentFailures = new ConcurrentLinkedDeque<>();
        LOG.info("CallsignValidationSampler initialized (sampleRate=1/{}, bufferSize={})",
                this.sampleRate, this.bufferSize);
    }

    /**
     * Records a validation failure, sampling according to the configured rate.
     *
     * <p>Only every Nth failure (based on sampleRate) is logged and stored.
     * All failures are counted for statistics.
     *
     * @param rawCallsign the original callsign value
     * @param failure the validation failure reason
     * @param source the data source name
     */
    public void recordFailure(String rawCallsign, Callsign.ValidationFailure failure, String source) {
        long count = totalFailures.incrementAndGet();

        // Sample every Nth failure
        if (count % sampleRate == 0) {
            sampledFailures.incrementAndGet();

            ValidationFailureRecord record = new ValidationFailureRecord(
                    rawCallsign, failure, source, Instant.now());

            // Add to buffer, trimming if needed
            recentFailures.addFirst(record);
            while (recentFailures.size() > bufferSize) {
                recentFailures.pollLast();
            }

            // Structured log for analysis
            LOG.info("Callsign validation failure (sampled): callsign={}, reason={}, source={}",
                    rawCallsign, failure.name(), source);
        }
    }

    /**
     * Returns recent sampled validation failures.
     *
     * <p>Returns a copy of the buffer to avoid concurrent modification issues.
     *
     * @return list of recent failures (most recent first)
     */
    public List<ValidationFailureRecord> getRecentFailures() {
        return new ArrayList<>(recentFailures);
    }

    /**
     * Returns recent sampled validation failures, limited to a maximum count.
     *
     * @param limit maximum number of failures to return
     * @return list of recent failures (most recent first)
     */
    public List<ValidationFailureRecord> getRecentFailures(int limit) {
        List<ValidationFailureRecord> result = new ArrayList<>(Math.min(limit, recentFailures.size()));
        int count = 0;
        for (ValidationFailureRecord record : recentFailures) {
            if (count >= limit) {
                break;
            }
            result.add(record);
            count++;
        }
        return result;
    }

    /**
     * Returns the total count of validation failures (not just sampled).
     *
     * @return total failure count
     */
    public long getTotalFailures() {
        return totalFailures.get();
    }

    /**
     * Returns the count of sampled failures (stored/logged).
     *
     * @return sampled failure count
     */
    public long getSampledFailures() {
        return sampledFailures.get();
    }

    /**
     * Clears all recorded failures and resets counters.
     *
     * <p>Primarily for testing.
     */
    public void clear() {
        recentFailures.clear();
        totalFailures.set(0);
        sampledFailures.set(0);
    }
}
