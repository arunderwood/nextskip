package io.nextskip.spots.model;

import java.time.Duration;
import java.util.Locale;

/**
 * Mode-specific time windows for band activity aggregation.
 *
 * <p>Different amateur radio modes have varying levels of activity and require
 * different aggregation windows to produce meaningful statistics:
 *
 * <ul>
 *   <li><b>FT8/FT4</b>: High volume digital modes with 15-second cycles.
 *       A 15-minute window captures hundreds of spots, providing good statistical
 *       significance.</li>
 *   <li><b>CW</b>: Lower volume than digital modes. A 30-minute window ensures
 *       enough samples for meaningful trend analysis.</li>
 *   <li><b>SSB</b>: Typically the lowest volume on PSKReporter (since it relies on
 *       WSJT-X integration). A 60-minute window is needed to accumulate sufficient
 *       data points.</li>
 * </ul>
 *
 * <p>The baseline window is used for trend calculation - it should be long enough
 * to establish a meaningful average but not so long that it includes different
 * propagation conditions (e.g., day/night transitions).
 */
public enum ModeWindow {

    /**
     * FT8 - The most popular digital mode on HF.
     *
     * <p>15-minute current window, 1-hour baseline (4 prior windows).
     */
    FT8(Duration.ofMinutes(15), Duration.ofHours(1)),

    /**
     * FT4 - Faster variant of FT8 for contest use.
     *
     * <p>Same window configuration as FT8.
     */
    FT4(Duration.ofMinutes(15), Duration.ofHours(1)),

    /**
     * CW - Morse code, lower volume on PSKReporter.
     *
     * <p>30-minute current window, 2-hour baseline.
     */
    CW(Duration.ofMinutes(30), Duration.ofHours(2)),

    /**
     * SSB - Single sideband voice, sparse on PSKReporter.
     *
     * <p>60-minute current window, 3-hour baseline.
     */
    SSB(Duration.ofMinutes(60), Duration.ofHours(3)),

    /**
     * Default for unknown or less common modes.
     *
     * <p>Uses conservative 30-minute window like CW.
     */
    DEFAULT(Duration.ofMinutes(30), Duration.ofHours(1));

    private final Duration currentWindow;
    private final Duration baselineWindow;

    ModeWindow(Duration currentWindow, Duration baselineWindow) {
        this.currentWindow = currentWindow;
        this.baselineWindow = baselineWindow;
    }

    /**
     * Returns the time window for current activity measurement.
     *
     * <p>This is the "now" window - how far back to look when counting
     * current spots on a band.
     *
     * @return duration of the current activity window
     */
    public Duration getCurrentWindow() {
        return currentWindow;
    }

    /**
     * Returns the time window for baseline/trend calculation.
     *
     * <p>This is used to calculate rolling average activity for comparison.
     * The baseline window should be a multiple of the current window to allow
     * for meaningful averaging.
     *
     * @return duration of the baseline window
     */
    public Duration getBaselineWindow() {
        return baselineWindow;
    }

    /**
     * Returns the number of current-window-sized periods in the baseline.
     *
     * <p>This is useful for calculating rolling averages:
     * baseline_avg = total_baseline_spots / getBaselineWindowCount()
     *
     * @return number of periods (e.g., 4 for FT8's 1hr baseline / 15min window)
     */
    public int getBaselineWindowCount() {
        return (int) (baselineWindow.toMinutes() / currentWindow.toMinutes());
    }

    /**
     * Returns the appropriate ModeWindow for a given mode string.
     *
     * <p>The lookup is case-insensitive. Unknown modes fall back to DEFAULT.
     *
     * <p>Supported mode variations:
     * <ul>
     *   <li>FT8, ft8</li>
     *   <li>FT4, ft4</li>
     *   <li>CW, cw</li>
     *   <li>SSB, ssb, USB, LSB, usb, lsb</li>
     * </ul>
     *
     * @param mode the mode string (e.g., "FT8", "CW")
     * @return the appropriate ModeWindow, never null
     */
    public static ModeWindow forMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return DEFAULT;
        }

        String normalized = mode.toUpperCase(Locale.ROOT).trim();

        // Handle SSB variants
        if ("USB".equals(normalized) || "LSB".equals(normalized)) {
            return SSB;
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }

    @Override
    public String toString() {
        return name() + "[current=" + currentWindow.toMinutes() + "m, "
                + "baseline=" + baselineWindow.toMinutes() + "m]";
    }
}
