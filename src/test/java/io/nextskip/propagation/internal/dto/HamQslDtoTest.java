package io.nextskip.propagation.internal.dto;

import io.nextskip.propagation.internal.InvalidApiResponseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for HamQslDto classes.
 */
class HamQslDtoTest {

    @Test
    void testHamQslData_NullSolardata() {
        HamQslDto.HamQslData data = new HamQslDto.HamQslData();
        data.setSolardata(null);

        assertNull(data.getSolardata());
        assertNull(data.getSolarFlux());
        assertNull(data.getAIndex());
        assertNull(data.getKIndex());
        assertNull(data.getSunspots());
    }

    @Test
    void testHamQslData_WithSolardata() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setSolarFlux(150.5);
        solarData.setAIndex(8);
        solarData.setKIndex(3);
        solarData.setSunspots(120);

        HamQslDto.HamQslData data = new HamQslDto.HamQslData();
        data.setSolardata(solarData);

        assertEquals(150.5, data.getSolarFlux(), 0.01);
        assertEquals(8, data.getAIndex());
        assertEquals(3, data.getKIndex());
        assertEquals(120, data.getSunspots());
    }

    @Test
    void testSolarData_Validate_AllNull() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        // All fields null should pass validation
        assertDoesNotThrow(solarData::validate);
    }

    @Test
    void testSolarData_Validate_ValidValues() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setSolarFlux(150.0);
        solarData.setAIndex(8);
        solarData.setKIndex(3);
        solarData.setSunspots(120);

        assertDoesNotThrow(solarData::validate);
    }

    @Test
    void testSolarData_Validate_KIndexTooHigh() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setKIndex(10);

        InvalidApiResponseException ex = assertThrows(InvalidApiResponseException.class,
                solarData::validate);
        assertEquals("HamQSL", ex.getApiName());
    }

    @Test
    void testSolarData_Validate_KIndexNegative() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setKIndex(-1);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testSolarData_Validate_AIndexTooHigh() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setAIndex(501);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testSolarData_Validate_AIndexNegative() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setAIndex(-1);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testSolarData_Validate_SolarFluxTooHigh() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setSolarFlux(1001.0);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testSolarData_Validate_SolarFluxNegative() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setSolarFlux(-1.0);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testSolarData_Validate_SunspotsTooHigh() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setSunspots(1001);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testSolarData_Validate_SunspotsNegative() {
        HamQslDto.SolarData solarData = new HamQslDto.SolarData();
        solarData.setSunspots(-1);

        assertThrows(InvalidApiResponseException.class, solarData::validate);
    }

    @Test
    void testCalculatedConditions_SettersGetters() {
        HamQslDto.BandConditionEntry entry1 = new HamQslDto.BandConditionEntry();
        entry1.setName("80m-40m");
        entry1.setTime("day");
        entry1.setValue("Good");

        HamQslDto.BandConditionEntry entry2 = new HamQslDto.BandConditionEntry();
        entry2.setName("30m-20m");
        entry2.setTime("night");
        entry2.setValue("Fair");

        HamQslDto.CalculatedConditions conditions = new HamQslDto.CalculatedConditions();
        conditions.setBands(List.of(entry1, entry2));

        assertEquals(2, conditions.getBands().size());
        assertEquals("80m-40m", conditions.getBands().get(0).getName());
        assertEquals("day", conditions.getBands().get(0).getTime());
        assertEquals("Good", conditions.getBands().get(0).getValue());
    }

    @Test
    void testBandConditionEntry_SettersGetters() {
        HamQslDto.BandConditionEntry entry = new HamQslDto.BandConditionEntry();
        entry.setName("12m-10m");
        entry.setTime("night");
        entry.setValue("Poor");

        assertEquals("12m-10m", entry.getName());
        assertEquals("night", entry.getTime());
        assertEquals("Poor", entry.getValue());
    }
}
