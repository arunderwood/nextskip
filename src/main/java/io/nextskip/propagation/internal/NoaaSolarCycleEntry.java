package io.nextskip.propagation.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for NOAA Space Weather Prediction Center solar cycle observation entry.
 *
 * Maps to individual entries from:
 * https://services.swpc.noaa.gov/json/solar-cycle/observed-solar-cycle-indices.json
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record NoaaSolarCycleEntry(
        @JsonProperty("time-tag") String timeTag,
        @JsonProperty("f10.7") Double solarFlux,
        @JsonProperty("ssn") Integer sunspotNumber
) {
    private static final String SOURCE_NAME = "NOAA";
    /**
     * Validates the data from the API response.
     *
     * @throws InvalidApiResponseException if data is invalid
     */
    public void validate() {
        // Solar flux typically ranges from ~50 to ~400
        if (solarFlux == null) {
            throw new InvalidApiResponseException(SOURCE_NAME, "Missing required field: f10.7 (solar flux)");
        }
        if (solarFlux < 0 || solarFlux > 1000) {
            throw new InvalidApiResponseException(SOURCE_NAME,
                    "Solar flux out of expected range [0, 1000]: " + solarFlux);
        }

        // Sunspot number typically ranges from 0 to ~400
        if (sunspotNumber == null) {
            throw new InvalidApiResponseException(SOURCE_NAME, "Missing required field: ssn (sunspot number)");
        }
        if (sunspotNumber < 0 || sunspotNumber > 1000) {
            throw new InvalidApiResponseException(SOURCE_NAME,
                    "Sunspot number out of expected range [0, 1000]: " + sunspotNumber);
        }

        // time-tag can be partial (e.g., "2025-11") so we don't validate format here
        if (timeTag == null || timeTag.isBlank()) {
            throw new InvalidApiResponseException(SOURCE_NAME, "Missing required field: time-tag");
        }
    }
}
