package io.nextskip.meteors.internal;

import io.nextskip.meteors.model.MeteorShower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeteorServiceImplTest {

    private static final String ACTIVE_SHOWER = "Active";

    @Mock
    private MeteorShowerDataLoader dataLoader;

    private MeteorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MeteorServiceImpl(dataLoader);
    }

    @Test
    void getMeteorShowers_filtersOutEnded() {
        Instant now = Instant.now();
        MeteorShower active = createShower(ACTIVE_SHOWER, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        MeteorShower ended = createShower("Ended", now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(5)));

        when(dataLoader.getShowers(anyInt())).thenReturn(List.of(active, ended));

        List<MeteorShower> result = service.getMeteorShowers();

        assertEquals(1, result.size());
        assertEquals(ACTIVE_SHOWER, result.get(0).name());
    }

    @Test
    void getPrimaryShower_returnsHighestScore() {
        Instant now = Instant.now();
        // Active at peak (high score)
        MeteorShower highScore = new MeteorShower(
                "High", "HI",
                now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(23)),
                now.minus(Duration.ofDays(2)), now.plus(Duration.ofDays(2)),
                150, null, null
        );
        // Upcoming (lower score)
        MeteorShower lowScore = new MeteorShower(
                "Low", "LO",
                now.plus(Duration.ofDays(5)), now.plus(Duration.ofDays(6)),
                now.plus(Duration.ofDays(3)), now.plus(Duration.ofDays(8)),
                50, null, null
        );

        when(dataLoader.getShowers(anyInt())).thenReturn(List.of(highScore, lowScore));

        var primary = service.getPrimaryShower();

        assertTrue(primary.isPresent());
        assertEquals("High", primary.get().name());
    }

    @Test
    void getActiveShowers_returnsOnlyActive() {
        Instant now = Instant.now();
        MeteorShower active = createShower(ACTIVE_SHOWER, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        MeteorShower upcoming = createShower("Upcoming", now.plus(Duration.ofDays(5)), now.plus(Duration.ofDays(10)));

        when(dataLoader.getShowers(anyInt())).thenReturn(List.of(active, upcoming));

        List<MeteorShower> result = service.getActiveShowers();

        assertEquals(1, result.size());
        assertEquals(ACTIVE_SHOWER, result.get(0).name());
    }

    private MeteorShower createShower(String name, Instant visStart, Instant visEnd) {
        return new MeteorShower(
                name, name.substring(0, Math.min(3, name.length())).toUpperCase(Locale.ROOT),
                visStart.plus(Duration.ofDays(1)), visStart.plus(Duration.ofDays(2)),
                visStart, visEnd,
                50, null, null
        );
    }
}
