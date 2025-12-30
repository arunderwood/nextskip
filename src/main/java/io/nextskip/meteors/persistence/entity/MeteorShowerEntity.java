package io.nextskip.meteors.persistence.entity;

import io.nextskip.meteors.model.MeteorShower;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for persisting meteor shower event data.
 *
 * <p>This entity maps to the domain model {@link MeteorShower} and provides
 * persistence for meteor shower events used in meteor scatter propagation planning.
 */
@Entity
@Table(name = "meteor_showers", indexes = {
    @Index(name = "idx_meteor_showers_peak_start", columnList = "peak_start"),
    @Index(name = "idx_meteor_showers_visibility_start", columnList = "visibility_start"),
    @Index(name = "idx_meteor_showers_code", columnList = "code")
})
public class MeteorShowerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "peak_start", nullable = false)
    private Instant peakStart;

    @Column(name = "peak_end", nullable = false)
    private Instant peakEnd;

    @Column(name = "visibility_start", nullable = false)
    private Instant visibilityStart;

    @Column(name = "visibility_end", nullable = false)
    private Instant visibilityEnd;

    @Column(name = "peak_zhr", nullable = false)
    private int peakZhr;

    @Column(name = "parent_body", length = 100)
    private String parentBody;

    @Column(name = "info_url", length = 500)
    private String infoUrl;

    /**
     * Default constructor required by JPA.
     */
    protected MeteorShowerEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new entity with all fields.
     *
     * @param name            display name with year
     * @param code            unique shower code
     * @param peakStart       start of peak activity window
     * @param peakEnd         end of peak activity window
     * @param visibilityStart start of visibility window
     * @param visibilityEnd   end of visibility window
     * @param peakZhr         zenithal hourly rate at peak
     * @param parentBody      parent comet or asteroid
     * @param infoUrl         URL for more information
     */
    public MeteorShowerEntity(String name, String code, Instant peakStart, Instant peakEnd,
                              Instant visibilityStart, Instant visibilityEnd, int peakZhr,
                              String parentBody, String infoUrl) {
        this.name = name;
        this.code = code;
        this.peakStart = peakStart;
        this.peakEnd = peakEnd;
        this.visibilityStart = visibilityStart;
        this.visibilityEnd = visibilityEnd;
        this.peakZhr = peakZhr;
        this.parentBody = parentBody;
        this.infoUrl = infoUrl;
    }

    /**
     * Creates an entity from a domain model.
     *
     * @param domain the domain model to convert
     * @return a new entity instance
     */
    public static MeteorShowerEntity fromDomain(MeteorShower domain) {
        return new MeteorShowerEntity(
                domain.name(),
                domain.code(),
                domain.peakStart(),
                domain.peakEnd(),
                domain.visibilityStart(),
                domain.visibilityEnd(),
                domain.peakZhr(),
                domain.parentBody(),
                domain.infoUrl()
        );
    }

    /**
     * Converts this entity to a domain model.
     *
     * @return the domain model representation
     */
    public MeteorShower toDomain() {
        return new MeteorShower(
                name,
                code,
                peakStart,
                peakEnd,
                visibilityStart,
                visibilityEnd,
                peakZhr,
                parentBody,
                infoUrl
        );
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Instant getPeakStart() {
        return peakStart;
    }

    public Instant getPeakEnd() {
        return peakEnd;
    }

    public Instant getVisibilityStart() {
        return visibilityStart;
    }

    public Instant getVisibilityEnd() {
        return visibilityEnd;
    }

    public int getPeakZhr() {
        return peakZhr;
    }

    public String getParentBody() {
        return parentBody;
    }

    public String getInfoUrl() {
        return infoUrl;
    }

    // Setters (for JPA)

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setPeakStart(Instant peakStart) {
        this.peakStart = peakStart;
    }

    public void setPeakEnd(Instant peakEnd) {
        this.peakEnd = peakEnd;
    }

    public void setVisibilityStart(Instant visibilityStart) {
        this.visibilityStart = visibilityStart;
    }

    public void setVisibilityEnd(Instant visibilityEnd) {
        this.visibilityEnd = visibilityEnd;
    }

    public void setPeakZhr(int peakZhr) {
        this.peakZhr = peakZhr;
    }

    public void setParentBody(String parentBody) {
        this.parentBody = parentBody;
    }

    public void setInfoUrl(String infoUrl) {
        this.infoUrl = infoUrl;
    }
}
