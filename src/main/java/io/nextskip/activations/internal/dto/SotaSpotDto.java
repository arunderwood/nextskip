package io.nextskip.activations.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for SOTA API spot response.
 *
 * <p>Maps to the JSON structure returned by https://api2.sota.org.uk/api/spots/50
 *
 * <p>Example response:
 * <pre>
 * {
 *   "id": 12345,
 *   "activatorCallsign": "W1ABC/P",
 *   "associationCode": "W7W",
 *   "summitCode": "W7W/LC-001",
 *   "frequency": "14.062",
 *   "mode": "CW",
 *   "summitDetails": "Mount Washington",
 *   "timeStamp": "2024-01-15T14:30:00",
 *   "comments": "QRV now"
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SotaSpotDto(
        @JsonProperty("id") Long id,
        @JsonProperty("activatorCallsign") String activatorCallsign,
        @JsonProperty("associationCode") String associationCode,
        @JsonProperty("summitCode") String summitCode,
        @JsonProperty("frequency") String frequency,
        @JsonProperty("mode") String mode,
        @JsonProperty("summitDetails") String summitDetails,
        @JsonProperty("timeStamp") String timeStamp,
        @JsonProperty("comments") String comments,
        @JsonProperty("callsign") String callsign,
        @JsonProperty("activatorName") String activatorName,
        @JsonProperty("highlander") Boolean highlander
) {
}
