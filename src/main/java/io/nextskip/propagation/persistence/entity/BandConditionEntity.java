package io.nextskip.propagation.persistence.entity;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for persisting band propagation condition data.
 *
 * <p>This entity maps to the domain model {@link BandCondition} and provides
 * persistence for band-by-band propagation conditions.
 *
 * <p>Note: The domain model doesn't include a timestamp, but we add one here
 * to track when the condition was recorded in the database.
 */
@Entity
@Table(name = "band_conditions", indexes = {
    @Index(name = "idx_band_conditions_band", columnList = "band"),
    @Index(name = "idx_band_conditions_recorded_at", columnList = "recorded_at DESC")
})
public class BandConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "band", nullable = false, length = 20)
    private FrequencyBand band;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false, length = 10)
    private BandConditionRating rating;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /**
     * Default constructor required by JPA.
     */
    protected BandConditionEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new entity with all required fields.
     *
     * @param band       the frequency band
     * @param rating     the condition rating
     * @param confidence confidence level (0.0-1.0)
     * @param notes      optional notes
     * @param recordedAt when this condition was recorded
     */
    public BandConditionEntity(FrequencyBand band, BandConditionRating rating,
                               double confidence, String notes, Instant recordedAt) {
        this.band = band;
        this.rating = rating;
        this.confidence = confidence;
        this.notes = notes;
        this.recordedAt = recordedAt;
    }

    /**
     * Creates an entity from a domain model.
     *
     * <p>Since the domain model doesn't have a timestamp, the current time is used.
     *
     * @param domain the domain model to convert
     * @return a new entity instance
     */
    public static BandConditionEntity fromDomain(BandCondition domain) {
        return fromDomain(domain, Instant.now());
    }

    /**
     * Creates an entity from a domain model with a specified timestamp.
     *
     * @param domain     the domain model to convert
     * @param recordedAt when this condition was recorded
     * @return a new entity instance
     */
    public static BandConditionEntity fromDomain(BandCondition domain, Instant recordedAt) {
        return new BandConditionEntity(
                domain.band(),
                domain.rating(),
                domain.confidence(),
                domain.notes(),
                recordedAt
        );
    }

    /**
     * Converts this entity to a domain model.
     *
     * <p>Note: The recordedAt timestamp is not included in the domain model.
     *
     * @return the domain model representation
     */
    public BandCondition toDomain() {
        return new BandCondition(band, rating, confidence, notes);
    }

    // Getters

    public Long getId() {
        return id;
    }

    public FrequencyBand getBand() {
        return band;
    }

    public BandConditionRating getRating() {
        return rating;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    // Setters (for JPA)

    public void setBand(FrequencyBand band) {
        this.band = band;
    }

    public void setRating(BandConditionRating rating) {
        this.rating = rating;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
