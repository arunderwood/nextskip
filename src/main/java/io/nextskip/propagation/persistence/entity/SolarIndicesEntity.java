package io.nextskip.propagation.persistence.entity;

import io.nextskip.propagation.model.SolarIndices;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for persisting solar indices data.
 *
 * <p>This entity maps to the domain model {@link SolarIndices} and provides
 * persistence for solar activity data used in HF propagation forecasting.
 */
@Entity
@Table(name = "solar_indices", indexes = {
    @Index(name = "idx_solar_indices_timestamp", columnList = "timestamp DESC")
})
public class SolarIndicesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solar_flux_index", nullable = false)
    private double solarFluxIndex;

    @Column(name = "a_index", nullable = false)
    private int aIndex;

    @Column(name = "k_index", nullable = false)
    private int kIndex;

    @Column(name = "sunspot_number", nullable = false)
    private int sunspotNumber;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    /**
     * Default constructor required by JPA.
     */
    protected SolarIndicesEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new entity with all required fields.
     *
     * @param solarFluxIndex Solar Flux Index at 10.7cm
     * @param aIndex         Planetary A-index
     * @param kIndex         Planetary K-index
     * @param sunspotNumber  Smoothed sunspot number
     * @param timestamp      When indices were observed
     * @param source         Data source identifier
     */
    public SolarIndicesEntity(double solarFluxIndex, int aIndex, int kIndex,
                              int sunspotNumber, Instant timestamp, String source) {
        this.solarFluxIndex = solarFluxIndex;
        this.aIndex = aIndex;
        this.kIndex = kIndex;
        this.sunspotNumber = sunspotNumber;
        this.timestamp = timestamp;
        this.source = source;
    }

    /**
     * Creates an entity from a domain model.
     *
     * @param domain the domain model to convert
     * @return a new entity instance
     */
    public static SolarIndicesEntity fromDomain(SolarIndices domain) {
        return new SolarIndicesEntity(
                domain.solarFluxIndex(),
                domain.aIndex(),
                domain.kIndex(),
                domain.sunspotNumber(),
                domain.timestamp(),
                domain.source()
        );
    }

    /**
     * Converts this entity to a domain model.
     *
     * @return the domain model representation
     */
    public SolarIndices toDomain() {
        return new SolarIndices(
                solarFluxIndex,
                aIndex,
                kIndex,
                sunspotNumber,
                timestamp,
                source
        );
    }

    // Getters

    public Long getId() {
        return id;
    }

    public double getSolarFluxIndex() {
        return solarFluxIndex;
    }

    public int getAIndex() {
        return aIndex;
    }

    public int getKIndex() {
        return kIndex;
    }

    public int getSunspotNumber() {
        return sunspotNumber;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    // Setters (for JPA)

    public void setSolarFluxIndex(double solarFluxIndex) {
        this.solarFluxIndex = solarFluxIndex;
    }

    public void setAIndex(int aIndex) {
        this.aIndex = aIndex;
    }

    public void setKIndex(int kIndex) {
        this.kIndex = kIndex;
    }

    public void setSunspotNumber(int sunspotNumber) {
        this.sunspotNumber = sunspotNumber;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
