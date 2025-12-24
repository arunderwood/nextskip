package io.nextskip.propagation.internal.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import io.nextskip.propagation.internal.InvalidApiResponseException;

import java.util.List;

/**
 * DTOs for parsing HamQSL XML response.
 *
 * <p>These classes map to the XML structure returned by hamqsl.com/solarxml.php:
 * <pre>{@code
 * <solar>
 *   <solardata>
 *     <solarflux>150.2</solarflux>
 *     <aindex>8</aindex>
 *     <kindex>3</kindex>
 *     <sunspots>120</sunspots>
 *     <calculatedconditions>
 *       <band name="80m-40m" time="day">Good</band>
 *       ...
 *     </calculatedconditions>
 *   </solardata>
 * </solar>
 * }</pre>
 */
public final class HamQslDto {

    private HamQslDto() {
        // Utility class - prevent instantiation
    }

    /**
     * Root element of HamQSL XML response.
     */
    @JacksonXmlRootElement(localName = "solar")
    public static class HamQslData {
        @JacksonXmlProperty(localName = "solardata")
        private SolarData solardata;

        public SolarData getSolardata() {
            return solardata;
        }

        public void setSolardata(SolarData solardata) {
            this.solardata = solardata;
        }

        public Double getSolarFlux() {
            return solardata != null ? solardata.getSolarFlux() : null;
        }

        public Integer getAIndex() {
            return solardata != null ? solardata.getAIndex() : null;
        }

        public Integer getKIndex() {
            return solardata != null ? solardata.getKIndex() : null;
        }

        public Integer getSunspots() {
            return solardata != null ? solardata.getSunspots() : null;
        }
    }

    /**
     * Solar data element containing indices and band conditions.
     */
    public static class SolarData {
        @JacksonXmlProperty(localName = "solarflux")
        private Double solarFlux;

        @JacksonXmlProperty(localName = "aindex")
        private Integer aIndex;

        @JacksonXmlProperty(localName = "kindex")
        private Integer kIndex;

        @JacksonXmlProperty(localName = "sunspots")
        private Integer sunspots;

        @JacksonXmlProperty(localName = "calculatedconditions")
        private CalculatedConditions calculatedConditions;

        public Double getSolarFlux() {
            return solarFlux;
        }

        public void setSolarFlux(Double solarFlux) {
            this.solarFlux = solarFlux;
        }

        public Integer getAIndex() {
            return aIndex;
        }

        public void setAIndex(Integer aIndex) {
            this.aIndex = aIndex;
        }

        public Integer getKIndex() {
            return kIndex;
        }

        public void setKIndex(Integer kIndex) {
            this.kIndex = kIndex;
        }

        public Integer getSunspots() {
            return sunspots;
        }

        public void setSunspots(Integer sunspots) {
            this.sunspots = sunspots;
        }

        public CalculatedConditions getCalculatedConditions() {
            return calculatedConditions;
        }

        public void setCalculatedConditions(CalculatedConditions calculatedConditions) {
            this.calculatedConditions = calculatedConditions;
        }

        /**
         * Validate the solar data fields.
         * Allows null values but checks ranges when present.
         *
         * @throws InvalidApiResponseException if values are out of expected ranges
         */
        public void validate() {
            // K-index ranges from 0 to 9
            if (kIndex != null && (kIndex < 0 || kIndex > 9)) {
                throw new InvalidApiResponseException("HamQSL",
                        "K-index out of expected range [0, 9]: " + kIndex);
            }

            // A-index typically ranges from 0 to ~400
            if (aIndex != null && (aIndex < 0 || aIndex > 500)) {
                throw new InvalidApiResponseException("HamQSL",
                        "A-index out of expected range [0, 500]: " + aIndex);
            }

            // Solar flux typically ranges from ~50 to ~400
            if (solarFlux != null && (solarFlux < 0 || solarFlux > 1000)) {
                throw new InvalidApiResponseException("HamQSL",
                        "Solar flux out of expected range [0, 1000]: " + solarFlux);
            }

            // Sunspot number typically ranges from 0 to ~400
            if (sunspots != null && (sunspots < 0 || sunspots > 1000)) {
                throw new InvalidApiResponseException("HamQSL",
                        "Sunspot number out of expected range [0, 1000]: " + sunspots);
            }
        }
    }

    /**
     * Container for band condition entries.
     */
    public static class CalculatedConditions {
        @JacksonXmlProperty(localName = "band")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<BandConditionEntry> bands;

        public List<BandConditionEntry> getBands() {
            return bands;
        }

        public void setBands(List<BandConditionEntry> bands) {
            this.bands = bands;
        }
    }

    /**
     * Individual band condition entry.
     *
     * <p>Example: {@code <band name="80m-40m" time="day">Good</band>}
     */
    public static class BandConditionEntry {
        @JacksonXmlProperty(isAttribute = true)
        private String name;

        @JacksonXmlProperty(isAttribute = true)
        private String time;

        @JacksonXmlText
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
