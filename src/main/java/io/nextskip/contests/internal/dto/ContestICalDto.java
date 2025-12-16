package io.nextskip.contests.internal.dto;

import java.time.Instant;

/**
 * Internal DTO representing raw contest data parsed from iCal VEVENT.
 *
 * <p>This DTO captures the basic fields available from the WA7BNM iCal feed:
 * <ul>
 *   <li>SUMMARY - contest name</li>
 *   <li>DTSTART - start time (UTC)</li>
 *   <li>DTEND - end time (UTC)</li>
 *   <li>URL - link to contest details page on contestcalendar.com</li>
 * </ul>
 *
 * <p>Additional metadata (bands, modes, sponsor, official rules URL) can be optionally
 * scraped from the contest details page referenced in the URL field.
 *
 * @param summary Contest name from SUMMARY field
 * @param startTime Event start from DTSTART field (UTC)
 * @param endTime Event end from DTEND field (UTC)
 * @param detailsUrl URL from URL field (contest details page)
 */
public record ContestICalDto(
        String summary,
        Instant startTime,
        Instant endTime,
        String detailsUrl
) {
    /**
     * Validates that required fields are present.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Contest summary (name) is required");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Contest start time is required");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("Contest end time is required");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Contest end time must be after start time");
        }
    }
}
