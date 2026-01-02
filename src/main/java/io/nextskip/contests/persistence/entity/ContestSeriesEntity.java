package io.nextskip.contests.persistence.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.common.model.FrequencyBand;
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
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for persisting WA7BNM contest series metadata.
 *
 * <p>Contest series represent the recurring contest definitions from WA7BNM
 * Contest Calendar. Each series is identified by a unique {@code wa7bnmRef}
 * (the {@code ref} parameter from contest detail page URLs).
 *
 * <p>This entity stores scraped metadata including permitted bands, modes,
 * exchange format, and links to official rules. The {@code revisionDate}
 * field enables change detection to avoid redundant scraping.
 *
 * <p>Bands and modes are stored using {@link ElementCollection} which creates
 * separate junction tables (contest_series_bands and contest_series_modes).
 */
@Entity
@Table(name = "contest_series", indexes = {
    @Index(name = "idx_contest_series_wa7bnm_ref", columnList = "wa7bnm_ref")
})
public class ContestSeriesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wa7bnm_ref", nullable = false, unique = true, length = 20)
    private String wa7bnmRef;

    @Column(name = "name", length = 200)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "contest_series_bands",
            joinColumns = @JoinColumn(name = "series_id")
    )
    @Column(name = "band", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<FrequencyBand> bands = EnumSet.noneOf(FrequencyBand.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "contest_series_modes",
            joinColumns = @JoinColumn(name = "series_id")
    )
    @Column(name = "mode", length = 20)
    private Set<String> modes = new HashSet<>();

    @Column(name = "sponsor", length = 100)
    private String sponsor;

    @Column(name = "official_rules_url", length = 500)
    private String officialRulesUrl;

    @Column(name = "exchange", length = 200)
    private String exchange;

    @Column(name = "cabrillo_name", length = 50)
    private String cabrilloName;

    @Column(name = "revision_date")
    private LocalDate revisionDate;

    @Column(name = "last_scraped_at")
    private Instant lastScrapedAt;

    /**
     * Default constructor required by JPA.
     */
    protected ContestSeriesEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new entity with all fields.
     *
     * @param wa7bnmRef        unique WA7BNM reference identifier
     * @param name             contest series name
     * @param bands            permitted frequency bands
     * @param modes            permitted operating modes
     * @param sponsor          sponsoring organization
     * @param officialRulesUrl URL to official contest rules
     * @param exchange         expected exchange format
     * @param cabrilloName     Cabrillo log contest identifier
     * @param revisionDate     page revision date for change detection
     * @param lastScrapedAt    when this data was last scraped
     */
    public ContestSeriesEntity(String wa7bnmRef, String name,
                               Set<FrequencyBand> bands, Set<String> modes,
                               String sponsor, String officialRulesUrl,
                               String exchange, String cabrilloName,
                               LocalDate revisionDate, Instant lastScrapedAt) {
        this.wa7bnmRef = wa7bnmRef;
        this.name = name;
        this.bands = createBandsSet(bands);
        this.modes = modes != null ? new HashSet<>(modes) : new HashSet<>();
        this.sponsor = sponsor;
        this.officialRulesUrl = officialRulesUrl;
        this.exchange = exchange;
        this.cabrilloName = cabrilloName;
        this.revisionDate = revisionDate;
        this.lastScrapedAt = lastScrapedAt;
    }

    private static Set<FrequencyBand> createBandsSet(Set<FrequencyBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return EnumSet.noneOf(FrequencyBand.class);
        }
        return EnumSet.copyOf(bands);
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getWa7bnmRef() {
        return wa7bnmRef;
    }

    public String getName() {
        return name;
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

    public String getOfficialRulesUrl() {
        return officialRulesUrl;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCabrilloName() {
        return cabrilloName;
    }

    public LocalDate getRevisionDate() {
        return revisionDate;
    }

    public Instant getLastScrapedAt() {
        return lastScrapedAt;
    }

    // Setters (for JPA)

    public void setWa7bnmRef(String wa7bnmRef) {
        this.wa7bnmRef = wa7bnmRef;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setOfficialRulesUrl(String officialRulesUrl) {
        this.officialRulesUrl = officialRulesUrl;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public void setCabrilloName(String cabrilloName) {
        this.cabrilloName = cabrilloName;
    }

    public void setRevisionDate(LocalDate revisionDate) {
        this.revisionDate = revisionDate;
    }

    public void setLastScrapedAt(Instant lastScrapedAt) {
        this.lastScrapedAt = lastScrapedAt;
    }
}
