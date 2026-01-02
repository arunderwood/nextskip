package io.nextskip.contests.persistence.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.model.Contest;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for persisting amateur radio contest event data.
 *
 * <p>This entity maps to the domain model {@link Contest} and provides
 * persistence for contest events with their permitted bands and modes.
 *
 * <p>Bands and modes are stored using {@link ElementCollection} which creates
 * separate junction tables (contest_bands and contest_modes).
 */
@Entity
@Table(name = "contests", indexes = {
    @Index(name = "idx_contests_start_time", columnList = "start_time"),
    @Index(name = "idx_contests_end_time", columnList = "end_time")
})
public class ContestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "contest_bands",
            joinColumns = @JoinColumn(name = "contest_id")
    )
    @Column(name = "band", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<FrequencyBand> bands = EnumSet.noneOf(FrequencyBand.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "contest_modes",
            joinColumns = @JoinColumn(name = "contest_id")
    )
    @Column(name = "mode", length = 20)
    private Set<String> modes = new HashSet<>();

    @Column(name = "sponsor", length = 100)
    private String sponsor;

    @Column(name = "calendar_source_url", length = 500)
    private String calendarSourceUrl;

    @Column(name = "official_rules_url", length = 500)
    private String officialRulesUrl;

    @Column(name = "wa7bnm_ref", length = 20)
    private String wa7bnmRef;

    /**
     * Default constructor required by JPA.
     */
    protected ContestEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new entity with all fields.
     *
     * @param name              contest name
     * @param startTime         when contest begins
     * @param endTime           when contest ends
     * @param bands             permitted frequency bands
     * @param modes             permitted modes
     * @param sponsor           sponsoring organization
     * @param calendarSourceUrl URL to calendar source
     * @param officialRulesUrl  URL to official rules
     */
    public ContestEntity(String name, Instant startTime, Instant endTime,
                         Set<FrequencyBand> bands, Set<String> modes,
                         String sponsor, String calendarSourceUrl, String officialRulesUrl) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bands = createBandsSet(bands);
        this.modes = modes != null ? new HashSet<>(modes) : new HashSet<>();
        this.sponsor = sponsor;
        this.calendarSourceUrl = calendarSourceUrl;
        this.officialRulesUrl = officialRulesUrl;
    }

    private static Set<FrequencyBand> createBandsSet(Set<FrequencyBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return EnumSet.noneOf(FrequencyBand.class);
        }
        return EnumSet.copyOf(bands);
    }

    /**
     * Creates an entity from a domain model.
     *
     * @param domain the domain model to convert
     * @return a new entity instance
     */
    public static ContestEntity fromDomain(Contest domain) {
        return new ContestEntity(
                domain.name(),
                domain.startTime(),
                domain.endTime(),
                domain.bands(),
                domain.modes(),
                domain.sponsor(),
                domain.calendarSourceUrl(),
                domain.officialRulesUrl()
        );
    }

    /**
     * Converts this entity to a domain model.
     *
     * <p>Creates defensive copies of the collections to preserve domain immutability.
     *
     * @return the domain model representation
     */
    public Contest toDomain() {
        return new Contest(
                name,
                startTime,
                endTime,
                Set.copyOf(bands),
                Set.copyOf(modes),
                sponsor,
                calendarSourceUrl,
                officialRulesUrl
        );
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "JPA requires mutable collection access")
    public Set<FrequencyBand> getBands() {
        return bands;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "JPA requires mutable collection access")
    public Set<String> getModes() {
        return modes;
    }

    public String getSponsor() {
        return sponsor;
    }

    public String getCalendarSourceUrl() {
        return calendarSourceUrl;
    }

    public String getOfficialRulesUrl() {
        return officialRulesUrl;
    }

    public String getWa7bnmRef() {
        return wa7bnmRef;
    }

    // Setters (for JPA)

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public void setBands(Set<FrequencyBand> bands) {
        this.bands = createBandsSet(bands);
    }

    public void setModes(Set<String> modes) {
        this.modes = modes != null ? new HashSet<>(modes) : new HashSet<>();
    }

    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }

    public void setCalendarSourceUrl(String calendarSourceUrl) {
        this.calendarSourceUrl = calendarSourceUrl;
    }

    public void setOfficialRulesUrl(String officialRulesUrl) {
        this.officialRulesUrl = officialRulesUrl;
    }

    public void setWa7bnmRef(String wa7bnmRef) {
        this.wa7bnmRef = wa7bnmRef;
    }
}
