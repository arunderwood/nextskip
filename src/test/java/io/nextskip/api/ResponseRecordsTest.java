package io.nextskip.api;

import io.nextskip.activations.api.ActivationsResponse;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.Park;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.api.ContestsResponse;
import io.nextskip.contests.model.Contest;
import io.nextskip.meteors.api.MeteorShowersResponse;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.propagation.api.PropagationResponse;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Response record compact constructors (defensive copying).
 *
 * <p>These tests verify that Response records properly handle null inputs
 * and create immutable copies of collections.
 */
class ResponseRecordsTest {

    @Test
    void testContestsResponse_NullContests() {
        ContestsResponse response = new ContestsResponse(null, 0, 0, 0, Instant.now());

        assertNotNull(response.contests());
        assertTrue(response.contests().isEmpty());
    }

    @Test
    void testContestsResponse_DefensiveCopy() {
        Contest contest = createTestContest("ARRL DX");
        ArrayList<Contest> mutableList = new ArrayList<>();
        mutableList.add(contest);

        ContestsResponse response = new ContestsResponse(mutableList, 1, 0, 1, Instant.now());

        // Verify list is immutable
        assertThrows(UnsupportedOperationException.class,
                () -> response.contests().add(createTestContest("CQ WW")));

        // Verify original list modification doesn't affect response
        mutableList.clear();
        assertEquals(1, response.contests().size());
    }

    @Test
    void testActivationsResponse_NullLists() {
        ActivationsResponse response = new ActivationsResponse(null, null, 0, Instant.now());

        assertNotNull(response.potaActivations());
        assertNotNull(response.sotaActivations());
        assertTrue(response.potaActivations().isEmpty());
        assertTrue(response.sotaActivations().isEmpty());
    }

    @Test
    void testActivationsResponse_DefensiveCopy() {
        Activation pota = createTestActivation(ActivationType.POTA);
        Activation sota = createTestActivation(ActivationType.SOTA);

        ArrayList<Activation> potaList = new ArrayList<>();
        potaList.add(pota);
        ArrayList<Activation> sotaList = new ArrayList<>();
        sotaList.add(sota);

        ActivationsResponse response = new ActivationsResponse(potaList, sotaList, 2, Instant.now());

        // Verify lists are immutable
        assertThrows(UnsupportedOperationException.class,
                () -> response.potaActivations().add(createTestActivation(ActivationType.POTA)));
        assertThrows(UnsupportedOperationException.class,
                () -> response.sotaActivations().add(createTestActivation(ActivationType.SOTA)));

        // Verify original list modification doesn't affect response
        potaList.clear();
        sotaList.clear();
        assertEquals(1, response.potaActivations().size());
        assertEquals(1, response.sotaActivations().size());
    }

    @Test
    void testMeteorShowersResponse_NullShowers() {
        MeteorShowersResponse response = new MeteorShowersResponse(null, 0, 0, null, Instant.now());

        assertNotNull(response.showers());
        assertTrue(response.showers().isEmpty());
    }

    @Test
    void testMeteorShowersResponse_DefensiveCopy() {
        MeteorShower shower = createTestMeteorShower("Perseids");
        ArrayList<MeteorShower> mutableList = new ArrayList<>();
        mutableList.add(shower);

        MeteorShowersResponse response = new MeteorShowersResponse(mutableList, 1, 0, shower, Instant.now());

        // Verify list is immutable
        assertThrows(UnsupportedOperationException.class,
                () -> response.showers().add(createTestMeteorShower("Geminids")));

        // Verify original list modification doesn't affect response
        mutableList.clear();
        assertEquals(1, response.showers().size());
    }

    @Test
    void testActivationsSummary_NullActivations() {
        ActivationsSummary summary = new ActivationsSummary(null, 0, 0, null);

        assertNotNull(summary.activations());
        assertTrue(summary.activations().isEmpty());
    }

    @Test
    void testActivationsSummary_DefensiveCopy() {
        Activation activation = createTestActivation(ActivationType.POTA);
        ArrayList<Activation> mutableList = new ArrayList<>();
        mutableList.add(activation);

        ActivationsSummary summary = new ActivationsSummary(mutableList, 1, 1, null);

        // Verify list is immutable
        assertThrows(UnsupportedOperationException.class,
                () -> summary.activations().add(createTestActivation(ActivationType.SOTA)));
    }

    @Test
    void testPropagationResponse_NullBandConditions() {
        SolarIndices indices = new SolarIndices(150.0, 8, 3, 120, Instant.now(), "TEST");
        PropagationResponse response = new PropagationResponse(indices, null, Instant.now());

        assertNotNull(response.bandConditions());
        assertTrue(response.bandConditions().isEmpty());
    }

    @Test
    void testPropagationResponse_DefensiveCopy() {
        SolarIndices indices = new SolarIndices(150.0, 8, 3, 120, Instant.now(), "TEST");
        ArrayList<BandCondition> mutableList = new ArrayList<>();

        PropagationResponse response = new PropagationResponse(indices, mutableList, Instant.now());

        // Verify list is immutable
        assertThrows(UnsupportedOperationException.class,
                () -> response.bandConditions().add(null));
    }

    // Helper methods

    private Contest createTestContest(String name) {
        return new Contest(
                name,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                java.util.Set.of(FrequencyBand.BAND_20M),
                java.util.Set.of("SSB"),
                "ARRL",
                "http://example.com/calendar",
                "http://example.com/rules"
        );
    }

    private Activation createTestActivation(ActivationType type) {
        Park park = new Park(
                "K-1234",
                "Test Park",
                "CO",
                "US",
                "DM79",
                39.7392,
                -104.9903
        );

        return new Activation(
                "spot-123",
                "W1ABC",
                type,
                14074.0,
                "FT8",
                Instant.now(),
                5,
                type.name(),
                park
        );
    }

    private MeteorShower createTestMeteorShower(String name) {
        Instant now = Instant.now();
        return new MeteorShower(
                name + " 2025",
                "PER",
                now,
                now.plusSeconds(86400),
                now.minusSeconds(86400),
                now.plusSeconds(172800),
                100,
                "Comet Swift-Tuttle",
                "http://example.com"
        );
    }
}
