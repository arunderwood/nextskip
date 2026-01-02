package io.nextskip.contests.internal.dto;

import io.nextskip.common.model.FrequencyBand;

import java.time.LocalDate;
import java.util.Set;

/**
 * Internal DTO representing scraped contest series data from WA7BNM detail pages.
 *
 * <p>This DTO captures metadata parsed from contest detail pages at
 * {@code https://contestcalendar.com/contestdetails.php?ref=N}. Fields include:
 * <ul>
 *   <li>bands - permitted frequency bands (parsed from "Bands:" field)</li>
 *   <li>modes - permitted operating modes (parsed from "Mode:" field)</li>
 *   <li>sponsor - sponsoring organization (parsed from "Sponsor:" field)</li>
 *   <li>officialRulesUrl - link to official rules (parsed from "Find rules at:" link)</li>
 *   <li>exchange - expected exchange format (parsed from "Exchange:" field)</li>
 *   <li>cabrilloName - Cabrillo log identifier (parsed from "Cabrillo name:" field)</li>
 *   <li>revisionDate - page revision date for change detection</li>
 * </ul>
 *
 * @param wa7bnmRef       unique WA7BNM reference identifier (from URL ref parameter)
 * @param name            contest series name
 * @param bands           permitted frequency bands
 * @param modes           permitted operating modes
 * @param sponsor         sponsoring organization
 * @param officialRulesUrl URL to official contest rules
 * @param exchange        expected exchange format
 * @param cabrilloName    Cabrillo log contest identifier
 * @param revisionDate    page revision date for change detection
 */
public record ContestSeriesDto(
        String wa7bnmRef,
        String name,
        Set<FrequencyBand> bands,
        Set<String> modes,
        String sponsor,
        String officialRulesUrl,
        String exchange,
        String cabrilloName,
        LocalDate revisionDate
) {
    /**
     * Compact constructor for defensive copying of mutable collections.
     */
    public ContestSeriesDto {
        bands = bands != null ? Set.copyOf(bands) : Set.of();
        modes = modes != null ? Set.copyOf(modes) : Set.of();
    }
}
