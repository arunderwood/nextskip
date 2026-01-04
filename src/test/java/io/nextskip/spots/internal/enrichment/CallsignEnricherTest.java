package io.nextskip.spots.internal.enrichment;

import io.nextskip.common.model.Callsign;
import io.nextskip.spots.model.Spot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CallsignEnricher}.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated callsign values
@ExtendWith(MockitoExtension.class)
class CallsignEnricherTest {

    @Mock
    private CallsignValidationSampler sampler;

    private CallsignEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new CallsignEnricher(sampler);
    }

    // ===========================================
    // enrich tests - basic
    // ===========================================

    @Test
    void testEnrich_NullSpot_ReturnsNull() {
        Spot result = enricher.enrich(null);

        assertThat(result).isNull();
    }

    @Test
    void testEnrich_ValidCallsigns_ReturnsSpotUnchanged() {
        Spot spot = createSpot("W1AW", "JA1ABC");

        Spot result = enricher.enrich(spot);

        assertThat(result).isSameAs(spot);
        verify(sampler, never()).recordFailure(anyString(), any(), anyString());
    }

    @Test
    void testEnrich_AlwaysReturnsSpot_PermissiveValidation() {
        Spot spot = createSpot("INVALID", "12345");

        Spot result = enricher.enrich(spot);

        assertThat(result).isSameAs(spot);
    }

    // ===========================================
    // enrich tests - invalid callsigns
    // ===========================================

    @Test
    void testEnrich_InvalidSpotterCall_RecordsFailure() {
        Spot spot = createSpot("12345", "W1AW"); // Spotter has no letter

        enricher.enrich(spot);

        verify(sampler).recordFailure(eq("12345"), eq(Callsign.ValidationFailure.NO_LETTER),
                eq("PSKReporter:spotter"));
    }

    @Test
    void testEnrich_InvalidSpottedCall_RecordsFailure() {
        Spot spot = createSpot("W1AW", "ABCDEF"); // Spotted has no digit

        enricher.enrich(spot);

        verify(sampler).recordFailure(eq("ABCDEF"), eq(Callsign.ValidationFailure.NO_DIGIT),
                eq("PSKReporter:spotted"));
    }

    @Test
    void testEnrich_BothCallsInvalid_RecordsBothFailures() {
        Spot spot = createSpot("Q1ABC", "12345"); // Q prefix and no letter

        enricher.enrich(spot);

        verify(sampler, times(2)).recordFailure(anyString(), any(), anyString());
    }

    @Test
    void testEnrich_QPrefix_RecordsQPrefixFailure() {
        Spot spot = createSpot("Q1ABC", "W1AW");

        enricher.enrich(spot);

        verify(sampler).recordFailure(eq("Q1ABC"), eq(Callsign.ValidationFailure.Q_PREFIX),
                eq("PSKReporter:spotter"));
    }

    // ===========================================
    // enrich tests - null/blank callsigns
    // ===========================================

    @Test
    void testEnrich_NullSpotterCall_SkipsValidation() {
        Spot spot = createSpot(null, "W1AW");

        Spot result = enricher.enrich(spot);

        assertThat(result).isSameAs(spot);
        verify(sampler, never()).recordFailure(anyString(), any(), anyString());
    }

    @Test
    void testEnrich_BlankSpottedCall_SkipsValidation() {
        Spot spot = createSpot("W1AW", "   ");

        Spot result = enricher.enrich(spot);

        assertThat(result).isSameAs(spot);
        verify(sampler, never()).recordFailure(anyString(), any(), anyString());
    }

    // ===========================================
    // enrich tests - source name
    // ===========================================

    @Test
    void testEnrich_NullSource_UsesUnknown() {
        Spot spot = new Spot(
                null, // null source
                "20m", "FT8", 14074000L, -10,
                Instant.now(), "12345", "FN31",
                null, "W1AW", "JO01", null, null
        );

        enricher.enrich(spot);

        ArgumentCaptor<String> sourceCaptor = ArgumentCaptor.forClass(String.class);
        verify(sampler).recordFailure(anyString(), any(), sourceCaptor.capture());
        assertThat(sourceCaptor.getValue()).isEqualTo("unknown:spotter");
    }

    @Test
    void testEnrich_CustomSource_UsesSourceName() {
        Spot spot = new Spot(
                "RBNGate",
                "20m", "CW", 14025000L, 20,
                Instant.now(), "12345", "FN31",
                null, "W1AW", "JO01", null, null
        );

        enricher.enrich(spot);

        ArgumentCaptor<String> sourceCaptor = ArgumentCaptor.forClass(String.class);
        verify(sampler).recordFailure(anyString(), any(), sourceCaptor.capture());
        assertThat(sourceCaptor.getValue()).isEqualTo("RBNGate:spotter");
    }

    // ===========================================
    // Helper methods
    // ===========================================

    private Spot createSpot(String spotterCall, String spottedCall) {
        return new Spot(
                "PSKReporter",
                "20m", "FT8", 14074000L, -10,
                Instant.now(), spotterCall, "FN31",
                null, spottedCall, "JO01", null, null
        );
    }
}
