package io.nextskip.test;

/**
 * Shared constants for test data across all test files.
 *
 * <p>Centralizes commonly used test values to reduce duplication and magic numbers.
 * All values should be realistic examples that match production data formats.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Constants class, not a test class
public final class TestConstants {

    // ==========================================================================
    // Callsigns
    // ==========================================================================

    /** Default amateur radio callsign for tests. */
    public static final String DEFAULT_CALLSIGN = "W1ABC";

    /** Secondary callsign for multi-activation tests. */
    public static final String SECONDARY_CALLSIGN = "K2DEF";

    /** Portable callsign suffix pattern. */
    public static final String PORTABLE_CALLSIGN = "W3XYZ/P";

    // ==========================================================================
    // POTA (Parks on the Air)
    // ==========================================================================

    /** Default POTA park reference code. */
    public static final String DEFAULT_PARK_REF = "US-0001";

    /** Default POTA park name. */
    public static final String DEFAULT_PARK_NAME = "Test Park";

    /** Default POTA state/region code. */
    public static final String DEFAULT_PARK_STATE = "CO";

    /** Default POTA country code. */
    public static final String DEFAULT_PARK_COUNTRY = "US";

    // ==========================================================================
    // SOTA (Summits on the Air)
    // ==========================================================================

    /** Default SOTA summit reference code. */
    public static final String DEFAULT_SUMMIT_REF = "W7W/LC-001";

    /** Default SOTA summit name. */
    public static final String DEFAULT_SUMMIT_NAME = "Test Summit";

    /** Default SOTA region code. */
    public static final String DEFAULT_SUMMIT_REGION = "WA";

    /** Default SOTA association code. */
    public static final String DEFAULT_SUMMIT_ASSOCIATION = "W7W";

    // ==========================================================================
    // Location and Coordinates
    // ==========================================================================

    /** Default latitude in decimal degrees (Boston area). */
    public static final double DEFAULT_LATITUDE = 42.5;

    /** Default longitude in decimal degrees (Boston area). */
    public static final double DEFAULT_LONGITUDE = -71.3;

    /** Default Maidenhead grid square. */
    public static final String DEFAULT_GRID = "FN42";

    // ==========================================================================
    // Radio Parameters
    // ==========================================================================

    /** Default operating frequency in kHz (20m band). */
    public static final double DEFAULT_FREQUENCY = 14250.0;

    /** Default operating mode. */
    public static final String DEFAULT_MODE = "SSB";

    /** Default QSO count for active stations. */
    public static final int DEFAULT_QSO_COUNT = 10;

    // ==========================================================================
    // Scoring
    // ==========================================================================

    /** Minimum possible score. */
    public static final int MIN_SCORE = 0;

    /** Maximum possible score. */
    public static final int MAX_SCORE = 100;

    /** Threshold for "hot" conditions. */
    public static final int HOT_THRESHOLD = 70;

    /** Threshold for "warm" conditions. */
    public static final int WARM_THRESHOLD = 45;

    /** Threshold for "neutral" conditions. */
    public static final int NEUTRAL_THRESHOLD = 20;

    // ==========================================================================
    // Solar Indices
    // ==========================================================================

    /** Default Solar Flux Index (good conditions). */
    public static final double DEFAULT_SFI = 100.0;

    /** Default planetary A-index. */
    public static final int DEFAULT_A_INDEX = 10;

    /** Default planetary K-index. */
    public static final int DEFAULT_K_INDEX = 2;

    /** Default sunspot number. */
    public static final int DEFAULT_SUNSPOT_NUMBER = 50;

    /** Default data source identifier. */
    public static final String DEFAULT_SOURCE = "Test Source";

    // ==========================================================================
    // Time Constants
    // ==========================================================================

    /** Duration in hours for "fresh" activations. */
    public static final int FRESH_HOURS = 1;

    /** Duration in hours for "stale" activations. */
    public static final int STALE_HOURS = 24;

    // ==========================================================================
    // Contests
    // ==========================================================================

    /** Default contest name. */
    public static final String DEFAULT_CONTEST_NAME = "Test Contest";

    /** Default contest sponsor. */
    public static final String DEFAULT_CONTEST_SPONSOR = "ARRL";

    // ==========================================================================
    // Meteor Showers
    // ==========================================================================

    /** Default meteor shower name. */
    public static final String DEFAULT_SHOWER_NAME = "Perseids 2025";

    /** Default meteor shower code. */
    public static final String DEFAULT_SHOWER_CODE = "PER";

    /** Default peak Zenithal Hourly Rate. */
    public static final int DEFAULT_PEAK_ZHR = 100;

    /** Default parent body. */
    public static final String DEFAULT_PARENT_BODY = "109P/Swift-Tuttle";

    /** Default info URL. */
    public static final String DEFAULT_INFO_URL = "https://imo.net";

    private TestConstants() {
        // Prevent instantiation
    }
}
