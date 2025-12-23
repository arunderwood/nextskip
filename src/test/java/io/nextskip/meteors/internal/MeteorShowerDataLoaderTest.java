package io.nextskip.meteors.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nextskip.meteors.model.MeteorShower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteorShowerDataLoaderTest {

    private MeteorShowerDataLoader dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = new MeteorShowerDataLoader(new ObjectMapper());
        dataLoader.init();
    }

    @Test
    void init_loadsTemplatesFromJson() {
        List<MeteorShowerTemplate> templates = dataLoader.getTemplates();

        assertFalse(templates.isEmpty(), "Should load templates from JSON");
        assertTrue(templates.size() >= 9, "Should have at least 9 major showers");
    }

    @Test
    void getShowers_returnsUpcomingAndActive() {
        List<MeteorShower> showers = dataLoader.getShowers(365); // Full year

        assertFalse(showers.isEmpty(), "Should return showers");

        // Verify all have required fields
        for (MeteorShower shower : showers) {
            assertNotNull(shower.name());
            assertNotNull(shower.code());
            assertNotNull(shower.peakStart());
            assertNotNull(shower.peakEnd());
            assertNotNull(shower.visibilityStart());
            assertNotNull(shower.visibilityEnd());
            assertTrue(shower.peakZhr() > 0);
        }
    }

    @Test
    void getShowers_handlesAnnualRecurrence() {
        List<MeteorShower> showers = dataLoader.getShowers(365);

        // Should include showers from both current and next year
        long distinctCodes = showers.stream()
                .map(MeteorShower::code)
                .distinct()
                .count();

        assertTrue(distinctCodes >= 9, "Should have all major showers represented");
    }

    @Test
    void getShowers_sortsByPeakDate() {
        List<MeteorShower> showers = dataLoader.getShowers(365);

        for (int i = 1; i < showers.size(); i++) {
            assertTrue(
                    !showers.get(i).peakStart().isBefore(showers.get(i - 1).peakStart()),
                    "Showers should be sorted by peak date"
            );
        }
    }
}
