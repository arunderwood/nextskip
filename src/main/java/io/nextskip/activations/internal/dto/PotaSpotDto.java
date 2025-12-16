package io.nextskip.activations.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for POTA API spot response.
 *
 * <p>Maps to the JSON structure returned by https://api.pota.app/spot/activator
 *
 * <p>Example response:
 * <pre>
 * {
 *   "spotId": 12345,
 *   "activator": "W1ABC",
 *   "reference": "K-0817",
 *   "frequency": "14.265",
 *   "mode": "SSB",
 *   "name": "Rocky Mountain National Park",
 *   "grid6": "DM79",
 *   "latitude": "40.3428",
 *   "longitude": "-105.6836",
 *   "spotTime": "2024-01-15T14:30:00Z",
 *   "qsos": 12
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PotaSpotDto(
        @JsonProperty("spotId") Long spotId,
        @JsonProperty("activator") String activator,
        @JsonProperty("reference") String reference,
        @JsonProperty("frequency") String frequency,
        @JsonProperty("mode") String mode,
        @JsonProperty("name") String name,
        @JsonProperty("locationDesc") String locationDesc,
        @JsonProperty("grid6") String grid6,
        @JsonProperty("latitude") String latitude,
        @JsonProperty("longitude") String longitude,
        @JsonProperty("spotTime") String spotTime,
        @JsonProperty("qsos") Integer qsos,
        @JsonProperty("spotter") String spotter,
        @JsonProperty("comments") String comments
) {
}
