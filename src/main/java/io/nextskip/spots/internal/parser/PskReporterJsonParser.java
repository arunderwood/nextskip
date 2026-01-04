package io.nextskip.spots.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.model.Spot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Parses PSKReporter MQTT JSON messages into Spot domain objects.
 *
 * <p>PSKReporter MQTT messages use abbreviated JSON keys:
 * <pre>{@code
 * {
 *   "sq": 30142870791,          // Sequence number
 *   "f": 21074653,              // Frequency in Hz
 *   "md": "FT8",                // Mode
 *   "rp": -5,                   // Signal report (SNR in dB)
 *   "t": 1662407712,            // Timestamp (decode time, Unix epoch)
 *   "t_tx": 1662407697,         // Transmission start time (optional)
 *   "sc": "SP2EWQ",             // Sender callsign (transmitting station)
 *   "sl": "JO93fn42",           // Sender locator (grid square)
 *   "rc": "CU3AT",              // Receiver callsign (receiving station)
 *   "rl": "HM68jp36",           // Receiver locator (grid square)
 *   "sa": 269,                  // Sender antenna azimuth (optional)
 *   "ra": 149,                  // Receiver antenna azimuth (optional)
 *   "b": "15m"                  // Band
 * }
 * }</pre>
 *
 * @see <a href="https://mqtt.pskreporter.info/">PSKReporter MQTT Documentation</a>
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed ObjectMapper is shared by design")
public class PskReporterJsonParser {

    private static final Logger LOG = LoggerFactory.getLogger(PskReporterJsonParser.class);
    private static final String SOURCE = "PSKReporter";
    private static final int MIN_GRID_LENGTH = 4;
    private static final int MAX_GRID_LENGTH = 6;

    private final ObjectMapper objectMapper;

    public PskReporterJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a PSKReporter MQTT JSON message into a Spot.
     *
     * <p>Returns {@code Optional.empty()} for invalid or malformed JSON.
     * Parsing errors are logged at debug level since some malformed data
     * is expected from the live feed.
     *
     * @param json the JSON message from MQTT
     * @return parsed Spot, or empty if parsing failed
     */
    public Optional<Spot> parse(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            return parseNode(node);
        } catch (JsonProcessingException e) {
            LOG.debug("Failed to parse PSKReporter JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Graceful handling of corrupted JSON data
    private Optional<Spot> parseNode(JsonNode node) {
        try {
            // Required fields for a valid spot
            String band = getTextOrNull(node, "b");
            String mode = getTextOrNull(node, "md");

            if (band == null || mode == null) {
                LOG.debug("Missing required fields (band or mode) in PSKReporter message");
                return Optional.empty();
            }

            // Timestamp - prefer t (decode time), fallback to t_tx
            Long timestamp = getLongOrNull(node, "t");
            if (timestamp == null) {
                timestamp = getLongOrNull(node, "t_tx");
            }
            if (timestamp == null) {
                LOG.debug("Missing timestamp in PSKReporter message");
                return Optional.empty();
            }
            Instant spottedAt = Instant.ofEpochSecond(timestamp);

            // Frequency in Hz
            Long frequencyHz = getLongOrNull(node, "f");

            // Signal report (SNR in dB)
            Integer snr = getIntOrNull(node, "rp");

            // Sender = transmitting station (the one being "spotted")
            String spottedCall = getTextOrNull(node, "sc");
            String spottedGrid = normalizeGrid(getTextOrNull(node, "sl"));

            // Receiver = receiving station (the "spotter")
            String spotterCall = getTextOrNull(node, "rc");
            String spotterGrid = normalizeGrid(getTextOrNull(node, "rl"));

            Spot spot = new Spot(
                    SOURCE,
                    band,
                    mode,
                    frequencyHz,
                    snr,
                    spottedAt,
                    spotterCall,
                    spotterGrid,
                    null,  // spotterContinent - enriched later
                    spottedCall,
                    spottedGrid,
                    null,  // spottedContinent - enriched later
                    null   // distanceKm - enriched later
            );

            return Optional.of(spot);

        } catch (RuntimeException e) {
            LOG.debug("Error parsing PSKReporter message: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Normalizes grid square to 4 or 6 character form.
     *
     * <p>PSKReporter sometimes sends 8-character extended grids.
     * We truncate to 6 characters for consistency with our schema.
     */
    private String normalizeGrid(String grid) {
        if (grid == null || grid.length() < MIN_GRID_LENGTH) {
            return grid;
        }
        // Truncate to 6 characters max (subsquare precision)
        if (grid.length() > MAX_GRID_LENGTH) {
            return grid.substring(0, MAX_GRID_LENGTH);
        }
        return grid;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String text = fieldNode.asText();
        return text.isBlank() ? null : text;
    }

    private Long getLongOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isNumber()) {
            return fieldNode.asLong();
        }
        // Try parsing string as number
        try {
            return Long.parseLong(fieldNode.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntOrNull(JsonNode node, String field) {
        Long value = getLongOrNull(node, field);
        return value != null ? value.intValue() : null;
    }
}
