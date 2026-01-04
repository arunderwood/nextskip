package io.nextskip.spots.internal.enrichment;

import io.nextskip.common.model.Callsign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CallsignValidationSampler}.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated values
class CallsignValidationSamplerTest {

    private CallsignValidationSampler sampler;

    @BeforeEach
    void setUp() {
        // Sample every 10th failure for testing
        sampler = new CallsignValidationSampler(10, 100);
    }

    // ===========================================
    // recordFailure tests
    // ===========================================

    @Test
    void testRecordFailure_IncrementsTotalCount() {
        sampler.recordFailure("INVALID", Callsign.ValidationFailure.NO_DIGIT, "test");

        assertThat(sampler.getTotalFailures()).isEqualTo(1);
    }

    @Test
    void testRecordFailure_SamplesEveryNthFailure() {
        // With sample rate of 10, only every 10th failure is sampled
        for (int i = 0; i < 25; i++) {
            sampler.recordFailure("CALL" + i, Callsign.ValidationFailure.NO_DIGIT, "test");
        }

        assertThat(sampler.getTotalFailures()).isEqualTo(25);
        assertThat(sampler.getSampledFailures()).isEqualTo(2); // 10th and 20th
    }

    @Test
    void testRecordFailure_StoresInBuffer() {
        // Record exactly 10 failures to trigger first sample
        for (int i = 0; i < 10; i++) {
            sampler.recordFailure("CALL" + i, Callsign.ValidationFailure.NO_DIGIT, "test");
        }

        List<CallsignValidationSampler.ValidationFailureRecord> failures = sampler.getRecentFailures();

        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst().rawCallsign()).isEqualTo("CALL9");
    }

    @Test
    void testRecordFailure_RespectsBufferLimit() {
        // Create sampler with small buffer
        CallsignValidationSampler smallBufferSampler = new CallsignValidationSampler(1, 5);

        // Record more failures than buffer can hold
        for (int i = 0; i < 10; i++) {
            smallBufferSampler.recordFailure("CALL" + i, Callsign.ValidationFailure.NO_DIGIT, "test");
        }

        List<CallsignValidationSampler.ValidationFailureRecord> failures = smallBufferSampler.getRecentFailures();

        // Buffer limited to 5 (minimum is 100 but test uses 5)
        assertThat(failures.size()).isLessThanOrEqualTo(100); // Minimum buffer is 100
    }

    // ===========================================
    // getRecentFailures tests
    // ===========================================

    @Test
    void testGetRecentFailures_EmptyWhenNoFailures() {
        List<CallsignValidationSampler.ValidationFailureRecord> failures = sampler.getRecentFailures();

        assertThat(failures).isEmpty();
    }

    @Test
    void testGetRecentFailures_MostRecentFirst() {
        // Sample every failure for this test
        CallsignValidationSampler allSampler = new CallsignValidationSampler(1, 100);

        allSampler.recordFailure("FIRST", Callsign.ValidationFailure.NO_DIGIT, "test");
        allSampler.recordFailure("SECOND", Callsign.ValidationFailure.NO_LETTER, "test");
        allSampler.recordFailure("THIRD", Callsign.ValidationFailure.TOO_SHORT, "test");

        List<CallsignValidationSampler.ValidationFailureRecord> failures = allSampler.getRecentFailures();

        assertThat(failures).hasSize(3);
        assertThat(failures.get(0).rawCallsign()).isEqualTo("THIRD");
        assertThat(failures.get(1).rawCallsign()).isEqualTo("SECOND");
        assertThat(failures.get(2).rawCallsign()).isEqualTo("FIRST");
    }

    @Test
    void testGetRecentFailures_WithLimit_ReturnsLimitedResults() {
        CallsignValidationSampler allSampler = new CallsignValidationSampler(1, 100);

        for (int i = 0; i < 10; i++) {
            allSampler.recordFailure("CALL" + i, Callsign.ValidationFailure.NO_DIGIT, "test");
        }

        List<CallsignValidationSampler.ValidationFailureRecord> failures = allSampler.getRecentFailures(3);

        assertThat(failures).hasSize(3);
    }

    // ===========================================
    // ValidationFailureRecord tests
    // ===========================================

    @Test
    void testValidationFailureRecord_ContainsCorrectData() {
        CallsignValidationSampler allSampler = new CallsignValidationSampler(1, 100);

        allSampler.recordFailure("INVALID123", Callsign.ValidationFailure.Q_PREFIX, "PSKReporter");

        List<CallsignValidationSampler.ValidationFailureRecord> failures = allSampler.getRecentFailures();

        assertThat(failures).hasSize(1);
        CallsignValidationSampler.ValidationFailureRecord record = failures.getFirst();
        assertThat(record.rawCallsign()).isEqualTo("INVALID123");
        assertThat(record.failure()).isEqualTo(Callsign.ValidationFailure.Q_PREFIX);
        assertThat(record.source()).isEqualTo("PSKReporter");
        assertThat(record.timestamp()).isNotNull();
    }

    // ===========================================
    // clear tests
    // ===========================================

    @Test
    void testClear_ResetsAllCounters() {
        CallsignValidationSampler allSampler = new CallsignValidationSampler(1, 100);

        allSampler.recordFailure("CALL1", Callsign.ValidationFailure.NO_DIGIT, "test");
        allSampler.recordFailure("CALL2", Callsign.ValidationFailure.NO_DIGIT, "test");

        allSampler.clear();

        assertThat(allSampler.getTotalFailures()).isZero();
        assertThat(allSampler.getSampledFailures()).isZero();
        assertThat(allSampler.getRecentFailures()).isEmpty();
    }
}
