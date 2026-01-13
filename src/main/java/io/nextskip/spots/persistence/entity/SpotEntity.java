package io.nextskip.spots.persistence.entity;

import io.nextskip.spots.model.Spot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.Instant;

/**
 * JPA entity for persisting PSKReporter spots.
 *
 * <p>Stores enriched spot data with indices optimized for:
 * <ul>
 *   <li>Band + time queries (recent spots on a band)</li>
 *   <li>Mode + time queries (recent spots by mode)</li>
 *   <li>Band + distance queries (DX spots by band)</li>
 *   <li>TTL cleanup (created_at for 24hr expiration)</li>
 * </ul>
 */
@Entity
@Table(name = "spots", indexes = {
        @Index(name = "idx_spots_band_time", columnList = "band, spotted_at DESC"),
        @Index(name = "idx_spots_mode_time", columnList = "mode, spotted_at DESC"),
        @Index(name = "idx_spots_distance", columnList = "band, distance_km DESC"),
        @Index(name = "idx_spots_created_at", columnList = "created_at")
})
public class SpotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "band", nullable = false, length = 10)
    private String band;

    @Column(name = "mode", nullable = false, length = 10)
    private String mode;

    @Column(name = "frequency_hz")
    private Long frequencyHz;

    @Column(name = "snr")
    private Integer snr;

    @Column(name = "spotted_at", nullable = false)
    private Instant spottedAt;

    @Column(name = "spotter_call", length = 30)
    private String spotterCall;

    @Column(name = "spotter_grid", length = 6)
    private String spotterGrid;

    @Column(name = "spotter_continent", length = 2)
    private String spotterContinent;

    @Column(name = "spotted_call", length = 30)
    private String spottedCall;

    @Column(name = "spotted_grid", length = 6)
    private String spottedGrid;

    @Column(name = "spotted_continent", length = 2)
    private String spottedContinent;

    @Column(name = "distance_km")
    private Integer distanceKm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Required by JPA.
     */
    protected SpotEntity() {
    }

    /**
     * Creates a SpotEntity from all field values.
     */
    public SpotEntity(
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
            Integer distanceKm,
            Instant createdAt
    ) {
        this.source = source;
        this.band = band;
        this.mode = mode;
        this.frequencyHz = frequencyHz;
        this.snr = snr;
        this.spottedAt = spottedAt;
        this.spotterCall = spotterCall;
        this.spotterGrid = spotterGrid;
        this.spotterContinent = spotterContinent;
        this.spottedCall = spottedCall;
        this.spottedGrid = spottedGrid;
        this.spottedContinent = spottedContinent;
        this.distanceKm = distanceKm;
        this.createdAt = createdAt;
    }

    /**
     * Converts a domain Spot to a persistence entity.
     *
     * <p>Sets createdAt to the current time using the system clock.
     *
     * @param spot the domain model
     * @return a new entity ready for persistence
     */
    public static SpotEntity fromDomain(Spot spot) {
        return fromDomain(spot, Clock.systemUTC());
    }

    /**
     * Converts a domain Spot to a persistence entity with a custom clock.
     *
     * <p>Allows injection of a test clock for deterministic testing.
     *
     * @param spot  the domain model
     * @param clock the clock to use for createdAt timestamp
     * @return a new entity ready for persistence
     */
    public static SpotEntity fromDomain(Spot spot, Clock clock) {
        return new SpotEntity(
                spot.source(),
                spot.band(),
                spot.mode(),
                spot.frequencyHz(),
                spot.snr(),
                spot.spottedAt(),
                spot.spotterCall(),
                spot.spotterGrid(),
                spot.spotterContinent(),
                spot.spottedCall(),
                spot.spottedGrid(),
                spot.spottedContinent(),
                spot.distanceKm(),
                clock.instant()
        );
    }

    /**
     * Converts this entity to a domain Spot.
     *
     * @return the domain model
     */
    public Spot toDomain() {
        return new Spot(
                source,
                band,
                mode,
                frequencyHz,
                snr,
                spottedAt,
                spotterCall,
                spotterGrid,
                spotterContinent,
                spottedCall,
                spottedGrid,
                spottedContinent,
                distanceKm
        );
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getBand() {
        return band;
    }

    public String getMode() {
        return mode;
    }

    public Long getFrequencyHz() {
        return frequencyHz;
    }

    public Integer getSnr() {
        return snr;
    }

    public Instant getSpottedAt() {
        return spottedAt;
    }

    public String getSpotterCall() {
        return spotterCall;
    }

    public String getSpotterGrid() {
        return spotterGrid;
    }

    public String getSpotterContinent() {
        return spotterContinent;
    }

    public String getSpottedCall() {
        return spottedCall;
    }

    public String getSpottedGrid() {
        return spottedGrid;
    }

    public String getSpottedContinent() {
        return spottedContinent;
    }

    public Integer getDistanceKm() {
        return distanceKm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters (for JPA)

    public void setId(Long id) {
        this.id = id;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setFrequencyHz(Long frequencyHz) {
        this.frequencyHz = frequencyHz;
    }

    public void setSnr(Integer snr) {
        this.snr = snr;
    }

    public void setSpottedAt(Instant spottedAt) {
        this.spottedAt = spottedAt;
    }

    public void setSpotterCall(String spotterCall) {
        this.spotterCall = spotterCall;
    }

    public void setSpotterGrid(String spotterGrid) {
        this.spotterGrid = spotterGrid;
    }

    public void setSpotterContinent(String spotterContinent) {
        this.spotterContinent = spotterContinent;
    }

    public void setSpottedCall(String spottedCall) {
        this.spottedCall = spottedCall;
    }

    public void setSpottedGrid(String spottedGrid) {
        this.spottedGrid = spottedGrid;
    }

    public void setSpottedContinent(String spottedContinent) {
        this.spottedContinent = spottedContinent;
    }

    public void setDistanceKm(Integer distanceKm) {
        this.distanceKm = distanceKm;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
