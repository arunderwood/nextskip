package io.nextskip.activations.persistence.entity;

import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationLocation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.activations.model.Summit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * JPA entity for persisting amateur radio activation spot data.
 *
 * <p>This entity denormalizes the polymorphic {@link ActivationLocation}
 * (Park or Summit) into flat columns to avoid complex inheritance mapping.
 * Location type is determined by the activation type (POTA uses Park fields,
 * SOTA uses Summit fields).
 *
 * <p>Design decision: Denormalization was chosen over polymorphic JPA strategies
 * (JOINED, TABLE_PER_CLASS, SINGLE_TABLE) to keep the schema simple and queries
 * straightforward. Park-specific fields are null for SOTA activations and vice versa.
 */
@Entity
@Table(name = "activations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_activation_spot_source",
                columnNames = {"spot_id", "source"}
        ),
        indexes = {
                @Index(name = "idx_activations_spotted_at", columnList = "spotted_at DESC"),
                @Index(name = "idx_activations_type_spotted", columnList = "type, spotted_at DESC"),
                @Index(name = "idx_activations_callsign", columnList = "activator_callsign")
        })
public class ActivationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_id", nullable = false, length = 100)
    private String spotId;

    @Column(name = "activator_callsign", nullable = false, length = 20)
    private String activatorCallsign;

    @Column(name = "type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ActivationType type;

    @Column(name = "frequency")
    private Double frequency;

    @Column(name = "mode", length = 20)
    private String mode;

    @Column(name = "spotted_at", nullable = false)
    private Instant spottedAt;

    @Column(name = "qso_count")
    private Integer qsoCount;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    // Common location fields (from ActivationLocation interface)

    @Column(name = "location_reference", nullable = false, length = 50)
    private String locationReference;

    @Column(name = "location_name", nullable = false, length = 200)
    private String locationName;

    @Column(name = "location_region_code", length = 10)
    private String locationRegionCode;

    // Park-specific fields (null for SOTA activations)

    @Column(name = "park_country_code", length = 10)
    private String parkCountryCode;

    @Column(name = "park_grid", length = 10)
    private String parkGrid;

    @Column(name = "park_latitude")
    private Double parkLatitude;

    @Column(name = "park_longitude")
    private Double parkLongitude;

    // Summit-specific fields (null for POTA activations)

    @Column(name = "summit_association_code", length = 20)
    private String summitAssociationCode;

    /**
     * Default constructor required by JPA.
     */
    protected ActivationEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new entity with all fields.
     *
     * @param spotId              unique spot identifier
     * @param activatorCallsign   operator's callsign
     * @param type                POTA or SOTA
     * @param frequency           operating frequency in kHz
     * @param mode                operating mode
     * @param spottedAt           when spotted
     * @param qsoCount            number of QSOs
     * @param source              data source identifier
     * @param locationReference   park/summit reference code
     * @param locationName        park/summit name
     * @param locationRegionCode  state/region code
     * @param parkCountryCode     country code (POTA only)
     * @param parkGrid            grid square (POTA only)
     * @param parkLatitude        latitude (POTA only)
     * @param parkLongitude       longitude (POTA only)
     * @param summitAssociationCode association code (SOTA only)
     */
    public ActivationEntity(String spotId, String activatorCallsign, ActivationType type,
                            Double frequency, String mode, Instant spottedAt, Integer qsoCount,
                            String source, String locationReference, String locationName,
                            String locationRegionCode, String parkCountryCode, String parkGrid,
                            Double parkLatitude, Double parkLongitude, String summitAssociationCode) {
        this.spotId = spotId;
        this.activatorCallsign = activatorCallsign;
        this.type = type;
        this.frequency = frequency;
        this.mode = mode;
        this.spottedAt = spottedAt;
        this.qsoCount = qsoCount;
        this.source = source;
        this.locationReference = locationReference;
        this.locationName = locationName;
        this.locationRegionCode = locationRegionCode;
        this.parkCountryCode = parkCountryCode;
        this.parkGrid = parkGrid;
        this.parkLatitude = parkLatitude;
        this.parkLongitude = parkLongitude;
        this.summitAssociationCode = summitAssociationCode;
    }

    /**
     * Creates an entity from a domain model.
     *
     * <p>Denormalizes the polymorphic ActivationLocation into flat columns based
     * on activation type. POTA activations populate park fields; SOTA activations
     * populate summit fields.
     *
     * @param domain the domain model to convert
     * @return a new entity instance
     */
    public static ActivationEntity fromDomain(Activation domain) {
        ActivationLocation location = domain.location();

        // Common location fields
        String locationReference = location.reference();
        String locationName = location.name();
        String locationRegionCode = location.regionCode();

        // Type-specific fields
        String parkCountryCode = null;
        String parkGrid = null;
        Double parkLatitude = null;
        Double parkLongitude = null;
        String summitAssociationCode = null;

        if (location instanceof Park park) {
            parkCountryCode = park.countryCode();
            parkGrid = park.grid();
            parkLatitude = park.latitude();
            parkLongitude = park.longitude();
        } else if (location instanceof Summit summit) {
            summitAssociationCode = summit.associationCode();
        }

        return new ActivationEntity(
                domain.spotId(),
                domain.activatorCallsign(),
                domain.type(),
                domain.frequency(),
                domain.mode(),
                domain.spottedAt(),
                domain.qsoCount(),
                domain.source(),
                locationReference,
                locationName,
                locationRegionCode,
                parkCountryCode,
                parkGrid,
                parkLatitude,
                parkLongitude,
                summitAssociationCode
        );
    }

    /**
     * Converts this entity to a domain model.
     *
     * <p>Reconstructs the polymorphic ActivationLocation based on the activation
     * type. POTA creates a Park; SOTA creates a Summit.
     *
     * @return the domain model representation
     */
    public Activation toDomain() {
        ActivationLocation location = createLocation();

        return new Activation(
                spotId,
                activatorCallsign,
                type,
                frequency,
                mode,
                spottedAt,
                qsoCount,
                source,
                location
        );
    }

    /**
     * Reconstructs the ActivationLocation from denormalized fields.
     */
    private ActivationLocation createLocation() {
        if (type == ActivationType.POTA) {
            return new Park(
                    locationReference,
                    locationName,
                    locationRegionCode,
                    parkCountryCode,
                    parkGrid,
                    parkLatitude,
                    parkLongitude
            );
        } else {
            return new Summit(
                    locationReference,
                    locationName,
                    locationRegionCode,
                    summitAssociationCode
            );
        }
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getSpotId() {
        return spotId;
    }

    public String getActivatorCallsign() {
        return activatorCallsign;
    }

    public ActivationType getType() {
        return type;
    }

    public Double getFrequency() {
        return frequency;
    }

    public String getMode() {
        return mode;
    }

    public Instant getSpottedAt() {
        return spottedAt;
    }

    public Integer getQsoCount() {
        return qsoCount;
    }

    public String getSource() {
        return source;
    }

    public String getLocationReference() {
        return locationReference;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocationRegionCode() {
        return locationRegionCode;
    }

    public String getParkCountryCode() {
        return parkCountryCode;
    }

    public String getParkGrid() {
        return parkGrid;
    }

    public Double getParkLatitude() {
        return parkLatitude;
    }

    public Double getParkLongitude() {
        return parkLongitude;
    }

    public String getSummitAssociationCode() {
        return summitAssociationCode;
    }

    // Setters (for JPA)

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public void setActivatorCallsign(String activatorCallsign) {
        this.activatorCallsign = activatorCallsign;
    }

    public void setType(ActivationType type) {
        this.type = type;
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setSpottedAt(Instant spottedAt) {
        this.spottedAt = spottedAt;
    }

    public void setQsoCount(Integer qsoCount) {
        this.qsoCount = qsoCount;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setLocationReference(String locationReference) {
        this.locationReference = locationReference;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setLocationRegionCode(String locationRegionCode) {
        this.locationRegionCode = locationRegionCode;
    }

    public void setParkCountryCode(String parkCountryCode) {
        this.parkCountryCode = parkCountryCode;
    }

    public void setParkGrid(String parkGrid) {
        this.parkGrid = parkGrid;
    }

    public void setParkLatitude(Double parkLatitude) {
        this.parkLatitude = parkLatitude;
    }

    public void setParkLongitude(Double parkLongitude) {
        this.parkLongitude = parkLongitude;
    }

    public void setSummitAssociationCode(String summitAssociationCode) {
        this.summitAssociationCode = summitAssociationCode;
    }
}
