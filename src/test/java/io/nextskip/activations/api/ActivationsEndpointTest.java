package io.nextskip.activations.api;

import io.nextskip.activations.model.Activation;
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
 * Unit tests for ActivationsEndpoint.
 *
 * <p>Since the endpoint is now a thin delegate to the service layer,
 * these tests verify that the endpoint correctly delegates to the service
 * and returns the service's response unchanged.
 */
@ExtendWith(MockitoExtension.class)
class ActivationsEndpointTest {

    @Mock
    private ActivationsService activationsService;

    private ActivationsEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new ActivationsEndpoint(activationsService);
    }

    @Test
    void should_ReturnActivationsResponseWithBothPotaAndSota() {
        // Given: Service returns response with both POTA and SOTA activations
        Instant now = Instant.now();
        List<Activation> potaActivations = List.of(
                createPotaActivation("1"),
                createPotaActivation("2")
        );
        List<Activation> sotaActivations = List.of(
                createSotaActivation("3"),
                createSotaActivation("4")
        );
        ActivationsResponse serviceResponse = new ActivationsResponse(
                potaActivations, sotaActivations, 4, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Should return service response
        assertNotNull(response);
        assertEquals(2, response.potaActivations().size(), "Should have 2 POTA activations");
        assertEquals(2, response.sotaActivations().size(), "Should have 2 SOTA activations");
        assertEquals(4, response.totalCount(), "Should have 4 total activations");
        assertEquals(now, response.lastUpdated());

        // Verify all POTA activations
        response.potaActivations().forEach(a ->
                assertEquals(ActivationType.POTA, a.type()));

        // Verify all SOTA activations
        response.sotaActivations().forEach(a ->
                assertEquals(ActivationType.SOTA, a.type()));

        verify(activationsService).getActivationsResponse();
    }

    @Test
    void should_ReturnOnlyPotaActivationsWhenNoSota() {
        // Given: Service returns response with only POTA activations
        Instant now = Instant.now();
        List<Activation> potaActivations = List.of(
                createPotaActivation("1"),
                createPotaActivation("2"),
                createPotaActivation("3")
        );
        ActivationsResponse serviceResponse = new ActivationsResponse(
                potaActivations, List.of(), 3, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Should return only POTA activations
        assertNotNull(response);
        assertEquals(3, response.potaActivations().size(), "Should have 3 POTA activations");
        assertEquals(0, response.sotaActivations().size(), "Should have 0 SOTA activations");
        assertEquals(3, response.totalCount(), "Should have 3 total activations");
        assertEquals(now, response.lastUpdated());

        verify(activationsService).getActivationsResponse();
    }

    @Test
    void should_ReturnOnlySotaActivationsWhenNoPota() {
        // Given: Service returns response with only SOTA activations
        Instant now = Instant.now();
        List<Activation> sotaActivations = List.of(
                createSotaActivation("1"),
                createSotaActivation("2")
        );
        ActivationsResponse serviceResponse = new ActivationsResponse(
                List.of(), sotaActivations, 2, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Should return only SOTA activations
        assertNotNull(response);
        assertEquals(0, response.potaActivations().size(), "Should have 0 POTA activations");
        assertEquals(2, response.sotaActivations().size(), "Should have 2 SOTA activations");
        assertEquals(2, response.totalCount(), "Should have 2 total activations");
        assertEquals(now, response.lastUpdated());

        verify(activationsService).getActivationsResponse();
    }

    @Test
    void should_ReturnEmptyListsWhenNoActivations() {
        // Given: Service returns empty response
        Instant now = Instant.now();
        ActivationsResponse serviceResponse = new ActivationsResponse(
                List.of(), List.of(), 0, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Should return empty lists
        assertNotNull(response);
        assertEquals(0, response.potaActivations().size(), "Should have 0 POTA activations");
        assertEquals(0, response.sotaActivations().size(), "Should have 0 SOTA activations");
        assertEquals(0, response.totalCount(), "Should have 0 total activations");
        assertEquals(now, response.lastUpdated());

        verify(activationsService).getActivationsResponse();
    }

    @Test
    void should_CalculateTotalCountCorrectly() {
        // Given: Service returns response with mixed activations
        Instant now = Instant.now();
        List<Activation> potaActivations = List.of(
                createPotaActivation("1"),
                createPotaActivation("3"),
                createPotaActivation("5")
        );
        List<Activation> sotaActivations = List.of(
                createSotaActivation("2"),
                createSotaActivation("4")
        );
        ActivationsResponse serviceResponse = new ActivationsResponse(
                potaActivations, sotaActivations, 5, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Total count should match sum of POTA and SOTA
        assertEquals(3, response.potaActivations().size());
        assertEquals(2, response.sotaActivations().size());
        assertEquals(5, response.totalCount(), "Total count should equal POTA + SOTA");

        verify(activationsService).getActivationsResponse();
    }

    @Test
    void should_PreserveActivationDetails() {
        // Given: Service returns activation with all details
        Instant now = Instant.now();
        io.nextskip.activations.model.Park park = new io.nextskip.activations.model.Park(
                "US-0001",
                "Test Park",
                "CO",
                "US",
                "FN42",
                42.5,
                -71.3
        );

        Activation detailed = new Activation(
                "12345",
                "W1ABC",
                ActivationType.POTA,
                14250.0,
                "SSB",
                now,
                15,
                "POTA API",
                park
        );
        ActivationsResponse serviceResponse = new ActivationsResponse(
                List.of(detailed), List.of(), 1, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Should preserve all activation details
        assertNotNull(response);
        assertEquals(1, response.potaActivations().size());

        Activation returned = response.potaActivations().get(0);
        assertEquals("12345", returned.spotId());
        assertEquals("W1ABC", returned.activatorCallsign());
        assertEquals(ActivationType.POTA, returned.type());
        assertEquals(14250.0, returned.frequency());
        assertEquals("SSB", returned.mode());
        assertEquals(now, returned.spottedAt());
        assertEquals(15, returned.qsoCount());
        assertEquals("POTA API", returned.source());

        // Verify location data
        assertNotNull(returned.location());
        assertEquals("US-0001", returned.location().reference());
        assertEquals("Test Park", returned.location().name());
        assertEquals("FN42", ((io.nextskip.activations.model.Park) returned.location()).grid());
        assertEquals(42.5, ((io.nextskip.activations.model.Park) returned.location()).latitude());
        assertEquals(-71.3, ((io.nextskip.activations.model.Park) returned.location()).longitude());

        verify(activationsService).getActivationsResponse();
    }

    @Test
    void should_HandleMixedActivationsWithCorrectCounts() {
        // Given: Uneven distribution of POTA and SOTA
        Instant now = Instant.now();
        List<Activation> potaActivations = List.of(
                createPotaActivation("1"),
                createPotaActivation("2"),
                createPotaActivation("3"),
                createPotaActivation("4"),
                createPotaActivation("5")
        );
        List<Activation> sotaActivations = List.of(
                createSotaActivation("6"),
                createSotaActivation("7")
        );
        ActivationsResponse serviceResponse = new ActivationsResponse(
                potaActivations, sotaActivations, 7, now);

        when(activationsService.getActivationsResponse()).thenReturn(serviceResponse);

        // When: Get activations
        ActivationsResponse response = endpoint.getActivations();

        // Then: Should correctly separate by type
        assertEquals(5, response.potaActivations().size());
        assertEquals(2, response.sotaActivations().size());
        assertEquals(7, response.totalCount());

        verify(activationsService).getActivationsResponse();
    }

    /**
     * Helper method to create a test POTA activation.
     */
    private Activation createPotaActivation(String id) {
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
                "W1ABC",
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
    private Activation createSotaActivation(String id) {
        io.nextskip.activations.model.Summit summit = new io.nextskip.activations.model.Summit(
                "W7W/LC-001",
                "Test Summit",
                "WA",
                "W7W"
        );

        return new Activation(
                id,
                "K2DEF/P",
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
