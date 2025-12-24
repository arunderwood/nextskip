package io.nextskip.propagation.api;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test for PropagationEndpoint.
 *
 * Tests the Hilla endpoint layer with mocked service.
 */
@ExtendWith(MockitoExtension.class)
class PropagationEndpointTest {

    @Mock
    private PropagationService propagationService;

    private PropagationEndpoint endpoint;

    private SolarIndices testSolarIndices;
    private List<BandCondition> testBandConditions;

    @BeforeEach
    void setUp() {
        endpoint = new PropagationEndpoint(propagationService);

        testSolarIndices = new SolarIndices(
                150.5,
                8,
                3,
                120,
                Instant.now(),
                "NOAA SWPC + HamQSL"
        );

        testBandConditions = List.of(
                new BandCondition(FrequencyBand.BAND_80M, BandConditionRating.FAIR),
                new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_15M, BandConditionRating.POOR),
                new BandCondition(FrequencyBand.BAND_10M, BandConditionRating.POOR)
        );
    }

    @Test
    void shouldGet_PropagationData_Success() {
        when(propagationService.getCurrentSolarIndices()).thenReturn(testSolarIndices);
        when(propagationService.getBandConditions()).thenReturn(testBandConditions);

        PropagationResponse response = endpoint.getPropagationData();

        assertNotNull(response);
        assertNotNull(response.solarIndices());
        assertNotNull(response.bandConditions());
        assertNotNull(response.timestamp());

        assertEquals(150.5, response.solarIndices().solarFluxIndex(), 0.01);
        assertEquals(5, response.bandConditions().size());

        verify(propagationService).getCurrentSolarIndices();
        verify(propagationService).getBandConditions();
    }

    @Test
    void shouldGet_PropagationData_NullSolarIndices() {
        when(propagationService.getCurrentSolarIndices()).thenReturn(null);
        when(propagationService.getBandConditions()).thenReturn(testBandConditions);

        PropagationResponse response = endpoint.getPropagationData();

        assertNotNull(response);
        assertNull(response.solarIndices());
        assertNotNull(response.bandConditions());
        assertEquals(5, response.bandConditions().size());

        verify(propagationService).getCurrentSolarIndices();
        verify(propagationService).getBandConditions();
    }

    @Test
    void shouldGet_PropagationData_EmptyBandConditions() {
        when(propagationService.getCurrentSolarIndices()).thenReturn(testSolarIndices);
        when(propagationService.getBandConditions()).thenReturn(List.of());

        PropagationResponse response = endpoint.getPropagationData();

        assertNotNull(response);
        assertNotNull(response.solarIndices());
        assertNotNull(response.bandConditions());
        assertTrue(response.bandConditions().isEmpty());

        verify(propagationService).getCurrentSolarIndices();
        verify(propagationService).getBandConditions();
    }

    @Test
    void shouldGet_PropagationData_BothNull() {
        when(propagationService.getCurrentSolarIndices()).thenReturn(null);
        when(propagationService.getBandConditions()).thenReturn(null);

        PropagationResponse response = endpoint.getPropagationData();

        assertNotNull(response);
        assertNull(response.solarIndices());
        // Defensive copying converts null to empty list
        assertNotNull(response.bandConditions());
        assertTrue(response.bandConditions().isEmpty());

        verify(propagationService).getCurrentSolarIndices();
        verify(propagationService).getBandConditions();
    }

    @Test
    void shouldGet_SolarIndices_Success() {
        when(propagationService.getCurrentSolarIndices()).thenReturn(testSolarIndices);

        SolarIndices result = endpoint.getSolarIndices();

        assertNotNull(result);
        assertEquals(150.5, result.solarFluxIndex(), 0.01);
        assertEquals(3, result.kIndex());
        assertEquals(8, result.aIndex());
        assertEquals(120, result.sunspotNumber());

        verify(propagationService).getCurrentSolarIndices();
    }

    @Test
    void shouldGet_SolarIndices_Null() {
        when(propagationService.getCurrentSolarIndices()).thenReturn(null);

        SolarIndices result = endpoint.getSolarIndices();

        assertNull(result);

        verify(propagationService).getCurrentSolarIndices();
    }

    @Test
    void shouldGet_BandConditions_Success() {
        when(propagationService.getBandConditions()).thenReturn(testBandConditions);

        List<BandCondition> result = endpoint.getBandConditions();

        assertNotNull(result);
        assertEquals(5, result.size());

        // Verify band ratings
        assertTrue(result.stream().anyMatch(bc ->
                bc.band() == FrequencyBand.BAND_20M && bc.rating() == BandConditionRating.GOOD));
        assertTrue(result.stream().anyMatch(bc ->
                bc.band() == FrequencyBand.BAND_15M && bc.rating() == BandConditionRating.POOR));

        verify(propagationService).getBandConditions();
    }

    @Test
    void shouldGet_BandConditions_Empty() {
        when(propagationService.getBandConditions()).thenReturn(List.of());

        List<BandCondition> result = endpoint.getBandConditions();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(propagationService).getBandConditions();
    }

    @Test
    void shouldReturn_ResponseTimestamp() throws InterruptedException {
        when(propagationService.getCurrentSolarIndices()).thenReturn(testSolarIndices);
        when(propagationService.getBandConditions()).thenReturn(testBandConditions);

        Instant before = Instant.now();
        Thread.sleep(10); // Small delay to ensure timestamp difference
        PropagationResponse response = endpoint.getPropagationData();
        Instant after = Instant.now();

        assertNotNull(response.timestamp());
        assertTrue(response.timestamp().isAfter(before));
        assertTrue(response.timestamp().isBefore(after));
    }

    @Test
    void shouldTest_SolarIndicesHelper_IsFavorable() {
        // Test the helper method in SolarIndices
        SolarIndices favorable = new SolarIndices(
                150.0, // High SFI
                5,     // Low A-index
                2,     // Low K-index
                100,
                Instant.now(),
                "Test"
        );

        assertTrue(favorable.isFavorable());

        SolarIndices unfavorable = new SolarIndices(
                80.0,  // Low SFI
                25,    // High A-index
                6,     // High K-index
                50,
                Instant.now(),
                "Test"
        );

        assertFalse(unfavorable.isFavorable());
    }

    @Test
    void shouldTest_BandConditionHelper_IsFavorable() {
        BandCondition good = new BandCondition(
                FrequencyBand.BAND_20M,
                BandConditionRating.GOOD,
                0.9
        );
        assertTrue(good.isFavorable());

        BandCondition fair = new BandCondition(
                FrequencyBand.BAND_40M,
                BandConditionRating.FAIR,
                0.8
        );
        assertFalse(fair.isFavorable());

        BandCondition poor = new BandCondition(
                FrequencyBand.BAND_15M,
                BandConditionRating.POOR,
                0.7
        );
        assertFalse(poor.isFavorable());

        BandCondition lowConfidence = new BandCondition(
                FrequencyBand.BAND_10M,
                BandConditionRating.GOOD,
                0.3  // Low confidence
        );
        assertFalse(lowConfidence.isFavorable());
    }

    @Test
    void shouldCalculate_BandConditionScore() {
        BandCondition good = new BandCondition(
                FrequencyBand.BAND_20M,
                BandConditionRating.GOOD,
                1.0
        );
        assertEquals(100, good.getScore());

        BandCondition fair = new BandCondition(
                FrequencyBand.BAND_40M,
                BandConditionRating.FAIR,
                0.5
        );
        assertEquals(30, fair.getScore()); // 60 * 0.5

        BandCondition poor = new BandCondition(
                FrequencyBand.BAND_15M,
                BandConditionRating.POOR,
                1.0
        );
        assertEquals(20, poor.getScore());

        BandCondition unknown = new BandCondition(
                FrequencyBand.BAND_10M,
                BandConditionRating.UNKNOWN,
                1.0
        );
        assertEquals(0, unknown.getScore());
    }
}
