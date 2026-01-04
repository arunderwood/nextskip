package io.nextskip.spots.model;

import java.time.Instant;

/**
 * Represents a PSKReporter spot - a single reception report of a radio transmission.
 *
 * <p>Spots are received from PSKReporter's MQTT feed and enriched with distance
 * and continent information before persistence.
 *
 * <p>This is an immutable domain model. Enrichment creates new instances with
 * additional fields populated.
 *
 * @param source           Data source identifier (e.g., "PSKReporter")
 * @param band             Band name (e.g., "20m", "40m", "10m")
 * @param mode             Operating mode (e.g., "FT8", "FT4", "CW")
 * @param frequencyHz      Frequency in Hz (e.g., 14074000 for 14.074 MHz)
 * @param snr              Signal-to-noise ratio in dB (can be negative)
 * @param spottedAt        When the spot was received by PSKReporter
 * @param spotterCall      Receiving station callsign
 * @param spotterGrid      Receiving station Maidenhead grid (4 or 6 chars)
 * @param spotterContinent Receiving station continent code (enriched, may be null)
 * @param spottedCall      Transmitting station callsign
 * @param spottedGrid      Transmitting station Maidenhead grid (4 or 6 chars)
 * @param spottedContinent Transmitting station continent code (enriched, may be null)
 * @param distanceKm       Distance between stations in km (enriched, may be null)
 */
public record Spot(
        String source,
        String band,
        String mode,
        Long frequencyHz,
        Integer snr,
        Instant spottedAt,
        String spotterCall,
        String spotterGrid,
        String spotterContinent,
        String spottedCall,
        String spottedGrid,
        String spottedContinent,
        Integer distanceKm
) {

    /**
     * Creates a new Spot with the specified distance.
     *
     * <p>Used by {@code DistanceEnricher} to add calculated distance.
     *
     * @param distanceKm distance in kilometers
     * @return new Spot with distance set
     */
    public Spot withDistance(Integer distanceKm) {
        return new Spot(
                source, band, mode, frequencyHz, snr, spottedAt,
                spotterCall, spotterGrid, spotterContinent,
                spottedCall, spottedGrid, spottedContinent,
                distanceKm
        );
    }

    /**
     * Creates a new Spot with the specified continent codes.
     *
     * <p>Used by {@code ContinentEnricher} to add derived continents.
     *
     * @param spotterContinent continent code for spotter (e.g., "NA", "EU")
     * @param spottedContinent continent code for spotted station
     * @return new Spot with continents set
     */
    public Spot withContinents(String spotterContinent, String spottedContinent) {
        return new Spot(
                source, band, mode, frequencyHz, snr, spottedAt,
                spotterCall, spotterGrid, spotterContinent,
                spottedCall, spottedGrid, spottedContinent,
                distanceKm
        );
    }
}
