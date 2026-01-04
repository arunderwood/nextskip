package io.nextskip.meteors.api;

import io.nextskip.meteors.model.MeteorShower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeteorEndpointTest {

    @Mock
    private MeteorService meteorService;

    private MeteorEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new MeteorEndpoint(meteorService);
    }

    @Test
    void getMeteorShowers_returnsResponse() {
        Instant now = Instant.now();
        MeteorShower active = new MeteorShower(
                "Perseids 2025", "PER",
                now.minus(Duration.ofHours(6)), now.plus(Duration.ofHours(18)),
                now.minus(Duration.ofDays(5)), now.plus(Duration.ofDays(3)),
                100, "109P/Swift-Tuttle", "https://imo.net"
        );
        MeteorShower upcoming = new MeteorShower(
                "Geminids 2025", "GEM",
                now.plus(Duration.ofDays(10)), now.plus(Duration.ofDays(11)),
                now.plus(Duration.ofDays(7)), now.plus(Duration.ofDays(13)),
                150, "3200 Phaethon", "https://imo.net"
        );

        when(meteorService.getMeteorShowers()).thenReturn(List.of(active, upcoming));

        MeteorShowersResponse response = endpoint.getMeteorShowers();

        assertNotNull(response);
        assertEquals(2, response.showers().size());
        assertEquals(1, response.activeCount());
        assertEquals(1, response.upcomingCount());
        assertNotNull(response.primaryShower());
        assertEquals("Perseids 2025", response.primaryShower().name());
        assertNotNull(response.lastUpdated());
    }

    @Test
    void getMeteorShowers_handlesNoShowers() {
        when(meteorService.getMeteorShowers()).thenReturn(List.of());

        MeteorShowersResponse response = endpoint.getMeteorShowers();

        assertNotNull(response);
        assertEquals(0, response.showers().size());
        assertEquals(0, response.activeCount());
        assertEquals(0, response.upcomingCount());
        assertNull(response.primaryShower());
    }
}
