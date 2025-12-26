package io.nextskip.propagation.internal;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PropagationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class PropagationServiceImplTest {

    @Mock
    private NoaaSwpcClient noaaClient;

    @Mock
    private HamQslSolarClient hamQslSolarClient;

    @Mock
    private HamQslBandClient hamQslBandClient;

    private PropagationServiceImpl service;

    private SolarIndices noaaData;
    private SolarIndices hamQslData;
    private List<BandCondition> bandConditions;

    @BeforeEach
    void setUp() {
        service = new PropagationServiceImpl(noaaClient, hamQslSolarClient, hamQslBandClient);

        // Setup test data
        noaaData = new SolarIndices(
                150.5,  // SFI from NOAA
                0,      // A-index (not provided by NOAA)
                0,      // K-index (not provided by NOAA)
                120,    // Sunspots from NOAA
                Instant.now(),
                "NOAA SWPC"
        );

        hamQslData = new SolarIndices(
                145.0,  // SFI (less accurate)
                8,      // A-index from HamQSL
                3,      // K-index from HamQSL
                115,    // Sunspots (less accurate)
                Instant.now(),
                "HamQSL"
        );

        bandConditions = List.of(
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR),
                new BandCondition(FrequencyBand.BAND_15M, BandConditionRating.POOR)
        );
    }

    @Test
    void shouldGet_CurrentSolarIndices_BothSourcesAvailable() {
        when(noaaClient.fetch()).thenReturn(noaaData);
        when(hamQslSolarClient.fetch()).thenReturn(hamQslData);

        SolarIndices result = service.getCurrentSolarIndices();

        assertNotNull(result);
        // Should prefer NOAA for SFI and sunspots
        assertEquals(150.5, result.solarFluxIndex(), 0.01);
        assertEquals(120, result.sunspotNumber());
        // Should use HamQSL for K and A index
        assertEquals(3, result.kIndex());
        assertEquals(8, result.aIndex());
        assertEquals("NOAA SWPC + HamQSL", result.source());

        verify(noaaClient).fetch();
        verify(hamQslSolarClient).fetch();
    }

    @Test
    void shouldGet_CurrentSolarIndices_OnlyNoaaAvailable() {
        when(noaaClient.fetch()).thenReturn(noaaData);
        when(hamQslSolarClient.fetch()).thenReturn(null);

        SolarIndices result = service.getCurrentSolarIndices();

        assertNotNull(result);
        assertEquals(noaaData, result);
        assertEquals("NOAA SWPC", result.source());

        verify(noaaClient).fetch();
        verify(hamQslSolarClient).fetch();
    }

    @Test
    void shouldGet_CurrentSolarIndices_OnlyHamQslAvailable() {
        when(noaaClient.fetch()).thenReturn(null);
        when(hamQslSolarClient.fetch()).thenReturn(hamQslData);

        SolarIndices result = service.getCurrentSolarIndices();

        assertNotNull(result);
        assertEquals(hamQslData, result);
        assertEquals("HamQSL", result.source());

        verify(noaaClient).fetch();
        verify(hamQslSolarClient).fetch();
    }

    @Test
    void shouldGet_CurrentSolarIndices_BothSourcesUnavailable() {
        when(noaaClient.fetch()).thenReturn(null);
        when(hamQslSolarClient.fetch()).thenReturn(null);

        SolarIndices result = service.getCurrentSolarIndices();

        assertNull(result);

        verify(noaaClient).fetch();
        verify(hamQslSolarClient).fetch();
    }

    @Test
    void shouldGet_CurrentSolarIndices_ExceptionHandling() {
        when(noaaClient.fetch()).thenThrow(new RuntimeException("Network error"));

        SolarIndices result = service.getCurrentSolarIndices();

        // Should handle exception gracefully and return null
        assertNull(result);

        verify(noaaClient).fetch();
    }

    @Test
    void shouldGet_BandConditions_Success() {
        when(hamQslBandClient.fetch()).thenReturn(bandConditions);

        List<BandCondition> result = service.getBandConditions();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(bandConditions, result);

        verify(hamQslBandClient).fetch();
    }

    @Test
    void shouldGet_BandConditions_EmptyList() {
        when(hamQslBandClient.fetch()).thenReturn(List.of());

        List<BandCondition> result = service.getBandConditions();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(hamQslBandClient).fetch();
    }

    @Test
    void shouldGet_BandConditions_ExceptionHandling() {
        when(hamQslBandClient.fetch())
                .thenThrow(new RuntimeException("Network error"));

        List<BandCondition> result = service.getBandConditions();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(hamQslBandClient).fetch();
    }

    @Test
    void shouldGet_BandCondition_Found() {
        when(hamQslBandClient.fetch()).thenReturn(bandConditions);

        BandCondition result = service.getBandCondition(FrequencyBand.BAND_20M);

        assertNotNull(result);
        assertEquals(FrequencyBand.BAND_20M, result.band());
        assertEquals(BandConditionRating.GOOD, result.rating());

        verify(hamQslBandClient).fetch();
    }

    @Test
    void shouldGet_BandCondition_NotFound() {
        when(hamQslBandClient.fetch()).thenReturn(bandConditions);

        BandCondition result = service.getBandCondition(FrequencyBand.BAND_160M);

        assertNull(result);

        verify(hamQslBandClient).fetch();
    }

    @Test
    void shouldGet_BandCondition_NullBand() {
        BandCondition result = service.getBandCondition(null);

        assertNull(result);

        verifyNoInteractions(hamQslBandClient);
    }

    @Test
    void shouldGet_SolarIndicesReactive() {
        when(noaaClient.fetch()).thenReturn(noaaData);
        when(hamQslSolarClient.fetch()).thenReturn(hamQslData);

        Mono<SolarIndices> resultMono = service.getSolarIndicesReactive();

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(150.5, result.solarFluxIndex(), 0.01);
                    assertEquals("NOAA SWPC + HamQSL", result.source());
                })
                .verifyComplete();

        verify(noaaClient).fetch();
        verify(hamQslSolarClient).fetch();
    }

    @Test
    void shouldGet_BandConditionsReactive() {
        when(hamQslBandClient.fetch()).thenReturn(bandConditions);

        Mono<List<BandCondition>> resultMono = service.getBandConditionsReactive();

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(3, result.size());
                })
                .verifyComplete();

        verify(hamQslBandClient).fetch();
    }
}
