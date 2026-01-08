package io.nextskip.common.model;

import java.util.Locale;

/**
 * Amateur radio frequency bands.
 *
 * <p>Represents the common HF and VHF amateur radio bands with their
 * frequency ranges in kHz and band-specific DX distance thresholds.
 *
 * <p>DX thresholds vary by band because propagation characteristics differ:
 * <ul>
 *   <li>160m: Difficult band - 3,000 km is exceptional (night skip required)</li>
 *   <li>20m: "Workhorse" DX band - 15,000 km is needed for excellent score</li>
 *   <li>6m: Sporadic-E at 2,000 km is exciting; F2 at 5,000+ km is legendary</li>
 * </ul>
 *
 * @see DxThresholds
 */
public enum FrequencyBand {
    // Low bands - difficult DX, lower thresholds
    BAND_160M("160m", 1800, 2000, new DxThresholds(3_000, 1_500, 500, "Difficult; night skip required")),
    BAND_80M("80m", 3500, 4000, new DxThresholds(5_000, 2_500, 1_000, "Regional; some DX at night")),
    BAND_60M("60m", 5330, 5405, new DxThresholds(6_000, 3_000, 1_500, "Secondary allocation; variable propagation")),

    // Transition bands
    BAND_40M("40m", 7000, 7300, new DxThresholds(7_000, 4_000, 2_000, "Day/night transitions")),
    BAND_30M("30m", 10100, 10150, new DxThresholds(8_000, 5_000, 2_500, "WARC; reliable propagation")),

    // High bands - workhorse DX bands, higher thresholds
    BAND_20M("20m", 14000, 14350, new DxThresholds(15_000, 10_000, 5_000, "Workhorse DX band")),
    BAND_17M("17m", 18068, 18168, new DxThresholds(12_000, 8_000, 4_000, "WARC; solar-dependent")),
    BAND_15M("15m", 21000, 21450, new DxThresholds(14_000, 9_000, 4_500, "Solar-dependent; excellent when open")),
    BAND_12M("12m", 24890, 24990, new DxThresholds(13_000, 8_500, 4_000, "WARC; similar to 10m/15m")),
    BAND_10M("10m", 28000, 29700, new DxThresholds(12_000, 7_000, 3_000, "Magic when open")),

    // VHF bands - rare propagation, low thresholds
    BAND_6M("6m", 50000, 54000, new DxThresholds(5_000, 2_000, 500, "Sporadic-E; rare F2")),
    BAND_2M("2m", 144000, 148000, new DxThresholds(2_000, 500, 100, "Tropo/EME"));

    private final String name;
    private final int startKhz;
    private final int endKhz;
    private final DxThresholds dxThresholds;

    FrequencyBand(String name, int startKhz, int endKhz, DxThresholds dxThresholds) {
        this.name = name;
        this.startKhz = startKhz;
        this.endKhz = endKhz;
        this.dxThresholds = dxThresholds;
    }

    /**
     * DX distance thresholds for scoring band activity.
     *
     * <p>Different bands have vastly different propagation characteristics.
     * These thresholds define what constitutes excellent, good, and moderate
     * DX distances for each band.
     *
     * <p>KEEP IN SYNC with src/main/frontend/utils/bandDxThresholds.ts
     *
     * @param excellentKm distance for 100 points (exceptional DX for this band)
     * @param goodKm distance for 70-100 point range
     * @param moderateKm distance for 40-70 point range
     * @param description human-readable description of band propagation
     */
    public record DxThresholds(int excellentKm, int goodKm, int moderateKm, String description) {

        /** Default thresholds for unknown bands (conservative, similar to 40m). */
        public static final DxThresholds DEFAULT = new DxThresholds(7_000, 4_000, 2_000, "Moderate propagation");
    }

    public String getName() {
        return name;
    }

    public int getStartKhz() {
        return startKhz;
    }

    public int getEndKhz() {
        return endKhz;
    }

    /**
     * Get the DX distance thresholds for this band.
     *
     * @return band-specific DX thresholds for scoring
     */
    public DxThresholds getDxThresholds() {
        return dxThresholds;
    }

    /**
     * Get the center frequency of the band in kHz.
     */
    public int getCenterKhz() {
        return (startKhz + endKhz) / 2;
    }

    /**
     * Check if a frequency (in kHz) falls within this band.
     *
     * @param freqKhz Frequency in kHz
     * @return true if the frequency is in this band
     */
    public boolean contains(int freqKhz) {
        return freqKhz >= startKhz && freqKhz <= endKhz;
    }

    /**
     * Check if a frequency (in Hz) falls within this band.
     *
     * @param freqHz Frequency in Hz
     * @return true if the frequency is in this band
     */
    public boolean containsHz(long freqHz) {
        long freqKhz = freqHz / 1000;
        return freqKhz >= startKhz && freqKhz <= endKhz;
    }

    /**
     * Find the band for a given frequency in kHz.
     *
     * @param freqKhz Frequency in kHz
     * @return The matching FrequencyBand, or null if no match
     */
    public static FrequencyBand fromFrequencyKhz(int freqKhz) {
        for (FrequencyBand band : values()) {
            if (band.contains(freqKhz)) {
                return band;
            }
        }
        return null;
    }

    /**
     * Find the band for a given frequency in Hz.
     *
     * @param freqHz Frequency in Hz
     * @return The matching FrequencyBand, or null if no match
     */
    public static FrequencyBand fromFrequencyHz(long freqHz) {
        return fromFrequencyKhz((int) (freqHz / 1000));
    }

    /**
     * Parse a band name string (e.g., "20m", "40M") to a FrequencyBand.
     *
     * @param bandName The band name string
     * @return The matching FrequencyBand, or null if no match
     */
    public static FrequencyBand fromString(String bandName) {
        if (bandName == null || bandName.isBlank()) {
            return null;
        }
        String normalized = bandName.toLowerCase(Locale.ROOT).trim();
        for (FrequencyBand band : values()) {
            if (band.name.equalsIgnoreCase(normalized)) {
                return band;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
