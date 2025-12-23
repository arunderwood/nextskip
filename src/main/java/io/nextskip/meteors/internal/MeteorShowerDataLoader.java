package io.nextskip.meteors.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nextskip.meteors.model.MeteorShower;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads meteor shower data from the curated JSON file and computes
 * concrete dates for the current and upcoming year.
 *
 * <p>The loader handles annual recurrence by calculating actual dates
 * based on the current date. It generates shower instances for:
 * <ul>
 *   <li>The current year (if not yet ended)</li>
 *   <li>The next year (for year-end planning)</li>
 * </ul>
 */
@Component
public class MeteorShowerDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorShowerDataLoader.class);
    private static final String DATA_FILE = "data/meteor-showers.json";

    private final ObjectMapper objectMapper;
    private List<MeteorShowerTemplate> templates;

    public MeteorShowerDataLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            loadTemplates();
            LOG.info("Loaded {} meteor shower templates", templates.size());
        } catch (IOException e) {
            LOG.error("Failed to load meteor shower data", e);
            templates = List.of();
        }
    }

    private void loadTemplates() throws IOException {
        ClassPathResource resource = new ClassPathResource(DATA_FILE);
        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode showersNode = root.get("showers");
            templates = objectMapper.readValue(
                    showersNode.traverse(),
                    new TypeReference<List<MeteorShowerTemplate>>() {}
            );
        }
    }

    /**
     * Get all meteor showers for the current period.
     *
     * <p>Returns showers that are:
     * <ul>
     *   <li>Currently active (within visibility window)</li>
     *   <li>Upcoming within the specified lookahead period</li>
     *   <li>Recently ended (within past 7 days, for reference)</li>
     * </ul>
     *
     * @param lookaheadDays number of days to look ahead for upcoming showers
     * @return list of MeteorShower instances with computed dates
     */
    public List<MeteorShower> getShowers(int lookaheadDays) {
        List<MeteorShower> result = new ArrayList<>();
        Instant now = Instant.now();
        Instant cutoffFuture = now.plus(Duration.ofDays(lookaheadDays));
        Instant cutoffPast = now.minus(Duration.ofDays(7)); // Include recently ended

        int currentYear = Year.now().getValue();

        for (MeteorShowerTemplate template : templates) {
            // Generate for current year
            MeteorShower currentYearShower = createShowerForYear(template, currentYear);
            if (isRelevant(currentYearShower, cutoffPast, cutoffFuture)) {
                result.add(currentYearShower);
            }

            // Generate for next year (for lookahead and year-end showers)
            MeteorShower nextYearShower = createShowerForYear(template, currentYear + 1);
            if (isRelevant(nextYearShower, cutoffPast, cutoffFuture)) {
                result.add(nextYearShower);
            }
        }

        // Sort by peak start time
        result.sort((a, b) -> a.peakStart().compareTo(b.peakStart()));

        return result;
    }

    private boolean isRelevant(MeteorShower shower, Instant cutoffPast, Instant cutoffFuture) {
        // Include if visibility window overlaps with our range
        return !shower.visibilityEnd().isBefore(cutoffPast)
                && !shower.visibilityStart().isAfter(cutoffFuture);
    }

    private MeteorShower createShowerForYear(MeteorShowerTemplate template, int year) {
        // Parse month-day
        String[] parts = template.peakMonthDay().split("-");
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);

        // Create peak start at midnight UTC on the peak day
        LocalDate peakDate = LocalDate.of(year, month, day);
        Instant peakStart = peakDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant peakEnd = peakStart.plus(Duration.ofHours(template.peakDurationHours()));

        // Calculate visibility window
        Instant visibilityStart = peakDate
                .plusDays(template.visibilityStartOffset())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant visibilityEnd = peakDate
                .plusDays(template.visibilityEndOffset())
                .atTime(23, 59, 59)
                .toInstant(ZoneOffset.UTC);

        // Name includes year for clarity
        String displayName = template.name() + " " + year;

        return new MeteorShower(
                displayName,
                template.code(),
                peakStart,
                peakEnd,
                visibilityStart,
                visibilityEnd,
                template.peakZhr(),
                template.parentBody(),
                template.infoUrl()
        );
    }

    /**
     * Get all loaded templates (for testing).
     */
    List<MeteorShowerTemplate> getTemplates() {
        return templates;
    }
}
