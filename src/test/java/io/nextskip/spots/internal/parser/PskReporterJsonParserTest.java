package io.nextskip.spots.internal.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nextskip.spots.model.Spot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static io.nextskip.test.TestConstants.FT8_20M_FREQUENCY_HZ;
import static io.nextskip.test.TestConstants.PSKREPORTER_SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PskReporterJsonParser}.
 *
 * <p>Tests JSON parsing of PSKReporter MQTT messages with abbreviated keys.
 */
class PskReporterJsonParserTest {

    private PskReporterJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new PskReporterJsonParser(new ObjectMapper());
    }

    @Test
    void testParse_ValidCompleteMessage_ReturnsSpot() throws IOException {
        String json = loadResource("valid-spot.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        Spot spot = result.get();
        assertThat(spot.source()).isEqualTo(PSKREPORTER_SOURCE);
        assertThat(spot.band()).isEqualTo("20m");
        assertThat(spot.mode()).isEqualTo("FT8");
        assertThat(spot.frequencyHz()).isEqualTo(FT8_20M_FREQUENCY_HZ);
        assertThat(spot.snr()).isEqualTo(-10);
        assertThat(spot.spottedAt()).isEqualTo(Instant.ofEpochSecond(1662407712));
        assertThat(spot.spottedCall()).isEqualTo("G3ABC");
        assertThat(spot.spottedGrid()).isEqualTo("JO01ab");
        assertThat(spot.spotterCall()).isEqualTo("W1AW");
        assertThat(spot.spotterGrid()).isEqualTo("FN31pr");
        // Continents and distance are enriched later, should be null
        assertThat(spot.spotterContinent()).isNull();
        assertThat(spot.spottedContinent()).isNull();
        assertThat(spot.distanceKm()).isNull();
    }

    @Test
    void testParse_MinimalMessage_ReturnsSpot() throws IOException {
        String json = loadResource("minimal-spot.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        Spot spot = result.get();
        assertThat(spot.band()).isEqualTo("20m");
        assertThat(spot.mode()).isEqualTo("FT8");
        assertThat(spot.spottedAt()).isEqualTo(Instant.ofEpochSecond(1662407712));
        // Optional fields should be null
        assertThat(spot.frequencyHz()).isNull();
        assertThat(spot.snr()).isNull();
        assertThat(spot.spottedCall()).isNull();
        assertThat(spot.spotterCall()).isNull();
    }

    @Test
    void testParse_MissingBand_ReturnsEmpty() throws IOException {
        String json = loadResource("missing-band.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_MissingMode_ReturnsEmpty() throws IOException {
        String json = loadResource("missing-mode.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_MissingTimestamp_ReturnsEmpty() {
        String json = """
            {"md": "FT8", "b": "20m"}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_FallbackToTxTimestamp_ReturnsSpot() throws IOException {
        String json = loadResource("fallback-timestamp.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        Spot spot = result.get();
        // Should use t_tx as fallback
        assertThat(spot.spottedAt()).isEqualTo(Instant.ofEpochSecond(1662407697));
    }

    @Test
    void testParse_NullJson_ReturnsEmpty() {
        Optional<Spot> result = parser.parse(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_BlankJson_ReturnsEmpty() {
        Optional<Spot> result = parser.parse("   ");

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_EmptyJson_ReturnsEmpty() {
        Optional<Spot> result = parser.parse("");

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_MalformedJson_ReturnsEmpty() {
        String json = "{ invalid json }";

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isEmpty();
    }

    @Test
    void testParse_ExtendedGrid_TruncatesToSix() throws IOException {
        String json = loadResource("extended-grid.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        Spot spot = result.get();
        // 8-character grids should be truncated to 6
        assertThat(spot.spottedGrid()).isEqualTo("JO01ab");
        assertThat(spot.spotterGrid()).isEqualTo("FN31pr");
    }

    @Test
    void testParse_SixCharGrid_Unchanged() {
        String json = """
            {"md": "FT8", "t": 1662407712, "b": "20m", "sl": "JO01ab", "rl": "FN31pr"}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        assertThat(result.get().spottedGrid()).isEqualTo("JO01ab");
        assertThat(result.get().spotterGrid()).isEqualTo("FN31pr");
    }

    @Test
    void testParse_FourCharGrid_Unchanged() {
        String json = """
            {"md": "FT8", "t": 1662407712, "b": "20m", "sl": "JO01", "rl": "FN31"}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        assertThat(result.get().spottedGrid()).isEqualTo("JO01");
        assertThat(result.get().spotterGrid()).isEqualTo("FN31");
    }

    @Test
    void testParse_ThreeCharGrid_ReturnedAsIs() {
        String json = """
            {"md": "FT8", "t": 1662407712, "b": "20m", "sl": "JO0"}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        // Short grids are returned as-is (validation happens elsewhere)
        assertThat(result.get().spottedGrid()).isEqualTo("JO0");
    }

    @Test
    void testParse_NegativeSnr_ParsedCorrectly() throws IOException {
        String json = loadResource("negative-snr.json");

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        assertThat(result.get().snr()).isEqualTo(-24);
    }

    @Test
    void testParse_PositiveSnr_ParsedCorrectly() {
        String json = """
            {"md": "FT8", "t": 1662407712, "b": "20m", "rp": 15}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        assertThat(result.get().snr()).isEqualTo(15);
    }

    @Test
    void testParse_NumericStringFields_HandledGracefully() {
        // Some fields might come as strings instead of numbers
        String json = """
            {"md": "FT8", "t": "1662407712", "b": "20m", "f": "14074000", "rp": "-10"}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        Spot spot = result.get();
        assertThat(spot.frequencyHz()).isEqualTo(FT8_20M_FREQUENCY_HZ);
        assertThat(spot.snr()).isEqualTo(-10);
    }

    @Test
    void testParse_EmptyStringFields_TreatedAsNull() {
        String json = """
            {"md": "FT8", "t": 1662407712, "b": "20m", "sc": "", "rc": "   "}
            """;

        Optional<Spot> result = parser.parse(json);

        assertThat(result).isPresent();
        Spot spot = result.get();
        assertThat(spot.spottedCall()).isNull();
        assertThat(spot.spotterCall()).isNull();
    }

    @Test
    void testParse_AllBands_Parsed() {
        String[] bands = {"160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m"};

        for (String band : bands) {
            String json = String.format("""
                {"md": "FT8", "t": 1662407712, "b": "%s"}
                """, band);

            Optional<Spot> result = parser.parse(json);

            assertThat(result)
                    .as("Band %s should be parsed", band)
                    .isPresent();
            assertThat(result.get().band()).isEqualTo(band);
        }
    }

    @Test
    void testParse_AllModes_Parsed() {
        String[] modes = {"FT8", "FT4", "CW", "SSB", "RTTY", "PSK31", "JS8"};

        for (String mode : modes) {
            String json = String.format("""
                {"md": "%s", "t": 1662407712, "b": "20m"}
                """, mode);

            Optional<Spot> result = parser.parse(json);

            assertThat(result)
                    .as("Mode %s should be parsed", mode)
                    .isPresent();
            assertThat(result.get().mode()).isEqualTo(mode);
        }
    }

    private String loadResource(String filename) throws IOException {
        Path path = Path.of("src/test/resources/spots", filename);
        return Files.readString(path);
    }
}
