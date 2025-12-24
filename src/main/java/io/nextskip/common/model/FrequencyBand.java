package io.nextskip.common.model;

import java.util.Locale;

/**
 * Amateur radio frequency bands.
 *
 * Represents the common HF and VHF amateur radio bands with their
 * frequency ranges in kHz.
 */
public enum FrequencyBand {
    BAND_160M("160m", 1800, 2000),
    BAND_80M("80m", 3500, 4000),
    BAND_60M("60m", 5330, 5405),
    BAND_40M("40m", 7000, 7300),
    BAND_30M("30m", 10100, 10150),
    BAND_20M("20m", 14000, 14350),
    BAND_17M("17m", 18068, 18168),
    BAND_15M("15m", 21000, 21450),
    BAND_12M("12m", 24890, 24990),
    BAND_10M("10m", 28000, 29700),
    BAND_6M("6m", 50000, 54000),
    BAND_2M("2m", 144000, 148000);

    private final String name;
    private final int startKhz;
    private final int endKhz;

    FrequencyBand(String name, int startKhz, int endKhz) {
        this.name = name;
        this.startKhz = startKhz;
        this.endKhz = endKhz;
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
