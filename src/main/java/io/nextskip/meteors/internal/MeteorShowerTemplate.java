package io.nextskip.meteors.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Template record for deserializing meteor shower data from the curated JSON file.
 *
 * <p>This template contains the static pattern data (month/day, ZHR, etc.)
 * that is used to compute actual {@code MeteorShower} instances with
 * concrete dates for the current/upcoming year.
 *
 * @param code Unique shower code (e.g., "PER" for Perseids)
 * @param name Human-readable name (e.g., "Perseids")
 * @param peakMonthDay Peak date in MM-DD format (e.g., "08-12")
 * @param peakDurationHours Duration of peak activity in hours
 * @param visibilityStartOffset Days before peak when shower becomes visible
 * @param visibilityEndOffset Days after peak when shower ends
 * @param peakZhr Zenithal Hourly Rate at peak
 * @param radiantRa Right ascension of radiant
 * @param radiantDec Declination of radiant
 * @param velocity Meteor velocity in km/s
 * @param parentBody Parent comet or asteroid
 * @param infoUrl URL for more information
 */
public record MeteorShowerTemplate(
        String code,
        String name,
        @JsonProperty("peakMonthDay") String peakMonthDay,
        @JsonProperty("peakDurationHours") int peakDurationHours,
        @JsonProperty("visibilityStartOffset") int visibilityStartOffset,
        @JsonProperty("visibilityEndOffset") int visibilityEndOffset,
        @JsonProperty("peakZhr") int peakZhr,
        @JsonProperty("radiantRa") String radiantRa,
        @JsonProperty("radiantDec") String radiantDec,
        int velocity,
        String parentBody,
        String infoUrl
) {
}
