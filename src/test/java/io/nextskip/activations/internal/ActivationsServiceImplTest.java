package io.nextskip.activations.internal;

import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.ActivationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActivationsServiceImpl.
 *
 * Tests the service layer that aggregates POTA and SOTA activations,
 * including error handling when clients fail.
 */
@ExtendWith(MockitoExtension.class)
class ActivationsServiceImplTest {

    @Mock
    private PotaClient potaClient;

    @Mock
    private SotaClient sotaClient;

    private ActivationsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivationsServiceImpl(potaClient, sotaClient);
    }

    @Test
    void shouldCombine_PotaAndSotaActivations() {
        // Given: Both clients return data
        List<Activation> potaActivations = List.of(
                createPotaActivation("1", "W1ABC"),
                createPotaActivation("2", "K2DEF")
        );
        List<Activation> sotaActivations = List.of(
                createSotaActivation("3", "W3XYZ/P")
        );

        when(potaClient.fetch()).thenReturn(potaActivations);
        when(sotaClient.fetch()).thenReturn(sotaActivations);

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then
        assertNotNull(summary);
        assertEquals(3, summary.activations().size(), "Should combine all activations");
        assertEquals(2, summary.potaCount(), "Should count POTA activations");
        assertEquals(1, summary.sotaCount(), "Should count SOTA activations");
        assertNotNull(summary.lastUpdated(), "Should set timestamp");

        // Verify both clients were called
        verify(potaClient).fetch();
        verify(sotaClient).fetch();
    }

    @Test
    void shouldHandle_PotaClientFailure() {
        // Given: POTA client throws exception, SOTA succeeds
        when(potaClient.fetch()).thenThrow(new RuntimeException("POTA API unavailable"));
        when(sotaClient.fetch()).thenReturn(List.of(createSotaActivation("1", "W1ABC/P")));

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then: Should gracefully degrade
        assertNotNull(summary);
        assertEquals(1, summary.activations().size(), "Should include SOTA activations");
        assertEquals(0, summary.potaCount(), "POTA count should be 0 on failure");
        assertEquals(1, summary.sotaCount(), "SOTA count should be 1");

        // Verify both clients were called
        verify(potaClient).fetch();
        verify(sotaClient).fetch();
    }

    @Test
    void shouldHandle_SotaClientFailure() {
        // Given: SOTA client throws exception, POTA succeeds
        when(potaClient.fetch()).thenReturn(List.of(createPotaActivation("1", "W1ABC")));
        when(sotaClient.fetch()).thenThrow(new RuntimeException("SOTA API unavailable"));

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then: Should gracefully degrade
        assertNotNull(summary);
        assertEquals(1, summary.activations().size(), "Should include POTA activations");
        assertEquals(1, summary.potaCount(), "POTA count should be 1");
        assertEquals(0, summary.sotaCount(), "SOTA count should be 0 on failure");

        verify(potaClient).fetch();
        verify(sotaClient).fetch();
    }

    @Test
    void shouldHandle_BothClientFailures() {
        // Given: Both clients fail
        when(potaClient.fetch()).thenThrow(new RuntimeException("POTA API down"));
        when(sotaClient.fetch()).thenThrow(new RuntimeException("SOTA API down"));

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then: Should return empty summary
        assertNotNull(summary);
        assertEquals(0, summary.activations().size(), "Should have no activations");
        assertEquals(0, summary.potaCount(), "POTA count should be 0");
        assertEquals(0, summary.sotaCount(), "SOTA count should be 0");
        assertNotNull(summary.lastUpdated(), "Should still set timestamp");

        verify(potaClient).fetch();
        verify(sotaClient).fetch();
    }

    @Test
    void shouldHandle_EmptyResults() {
        // Given: Both clients return empty lists
        when(potaClient.fetch()).thenReturn(List.of());
        when(sotaClient.fetch()).thenReturn(List.of());

        // When
        ActivationsSummary summary = service.getActivationsSummary();

        // Then
        assertNotNull(summary);
        assertEquals(0, summary.activations().size());
        assertEquals(0, summary.potaCount());
        assertEquals(0, summary.sotaCount());
        assertNotNull(summary.lastUpdated());
    }

    @Test
    void shouldSet_RecentLastUpdatedTimestamp() {
        // Given
        Instant before = Instant.now();
        when(potaClient.fetch()).thenReturn(List.of());
        when(sotaClient.fetch()).thenReturn(List.of());

        // When
        ActivationsSummary summary = service.getActivationsSummary();
        Instant after = Instant.now();

        // Then: Timestamp should be between before and after
        assertNotNull(summary.lastUpdated());
        assertTrue(
                summary.lastUpdated().equals(before) || summary.lastUpdated().isAfter(before),
                "Timestamp should be at or after start time"
        );
        assertTrue(
                summary.lastUpdated().equals(after) || summary.lastUpdated().isBefore(after),
                "Timestamp should be at or before end time"
        );
    }

    /**
     * Helper method to create a test POTA activation.
     */
    private Activation createPotaActivation(String id, String callsign) {
        io.nextskip.activations.model.Park park = new io.nextskip.activations.model.Park(
                "US-0001",
                "Test Park",
                "CO",
                "US",
                "FN42",
                42.5,
                -71.3
        );

        return new Activation(
                id,
                callsign,
                ActivationType.POTA,
                14250.0,
                "SSB",
                Instant.now(),
                10,
                "POTA API",
                park
        );
    }

    /**
     * Helper method to create a test SOTA activation.
     */
    private Activation createSotaActivation(String id, String callsign) {
        io.nextskip.activations.model.Summit summit = new io.nextskip.activations.model.Summit(
                "W7W/LC-001",
                "Test Summit",
                "WA",
                "W7W"
        );

        return new Activation(
                id,
                callsign,
                ActivationType.SOTA,
                7200.0,
                "CW",
                Instant.now(),
                null,
                "SOTA API",
                summit
        );
    }
}
