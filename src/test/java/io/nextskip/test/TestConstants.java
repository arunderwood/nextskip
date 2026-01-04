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

    /** Reference timestamp for deterministic time-based tests. */
    public static final String REFERENCE_TIME_STRING = "2025-01-15T12:00:00Z";

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

    // ==========================================================================
    // Invariant Testing - Activation Age Decay Thresholds (minutes)
    // ==========================================================================

    /** Age threshold for very fresh activations (0-5 minutes = max score). */
    public static final long ACTIVATION_VERY_FRESH_THRESHOLD = 5;

    /** Age threshold for fresh activations (5-15 minutes = decaying). */
    public static final long ACTIVATION_FRESH_THRESHOLD = 15;

    /** Age threshold for aging activations (15-30 minutes = faster decay). */
    public static final long ACTIVATION_AGING_THRESHOLD = 30;

    /** Age threshold for stale activations (30-60 minutes = approaching zero). */
    public static final long ACTIVATION_STALE_THRESHOLD = 60;

    // ==========================================================================
    // Invariant Testing - Band Condition Rating Bases
    // ==========================================================================

    /** Base score multiplier for GOOD rating (100%). */
    public static final int RATING_GOOD_BASE = 100;

    /** Base score multiplier for FAIR rating (60%). */
    public static final int RATING_FAIR_BASE = 60;

    /** Base score multiplier for POOR rating (20%). */
    public static final int RATING_POOR_BASE = 20;

    /** Base score multiplier for UNKNOWN rating (0%). */
    public static final int RATING_UNKNOWN_BASE = 0;

    // ==========================================================================
    // Invariant Testing - Solar Indices Thresholds
    // ==========================================================================

    /** K-index threshold for favorable conditions (must be < this value). */
    public static final int K_INDEX_FAVORABLE_THRESHOLD = 4;

    /** SFI threshold for favorable conditions (must be > this value). */
    public static final double SFI_FAVORABLE_THRESHOLD = 100.0;

    /** A-index threshold for favorable conditions (must be < this value). */
    public static final int A_INDEX_FAVORABLE_THRESHOLD = 20;

    // ==========================================================================
    // Invariant Testing - Contest Time Windows (hours)
    // ==========================================================================

    /** Contest is imminent when starting within this many hours. */
    public static final long CONTEST_IMMINENT_HOURS = 6;

    /** Contest is soon when starting within this many hours. */
    public static final long CONTEST_SOON_HOURS = 24;

    /** Contest is upcoming when starting within this many hours. */
    public static final long CONTEST_UPCOMING_HOURS = 72;

    // ==========================================================================
    // PSKReporter / Spots
    // ==========================================================================

    /** Default PSKReporter source name. */
    public static final String PSKREPORTER_SOURCE = "PSKReporter";

    /** Default FT8 frequency on 20m in Hz. */
    public static final long FT8_20M_FREQUENCY_HZ = 14074000L;

    /** Default signal-to-noise ratio. */
    public static final int DEFAULT_SNR = -10;

    /** Trans-Atlantic distance in km (FN31 to JO01). */
    public static final int TRANSATLANTIC_DISTANCE_KM = 5500;

    /** Trans-Pacific distance in km (CM97 to PM95). */
    public static final int TRANSPACIFIC_DISTANCE_KM = 8500;

    /** Default spot TTL in hours. */
    public static final int SPOT_TTL_HOURS = 24;

    /** Spot batch size for persistence. */
    public static final int SPOT_BATCH_SIZE = 100;

    /** Grid square tolerance for distance verification (km). */
    public static final int DISTANCE_TOLERANCE_KM = 100;

    /** North American grid square (Boston area). */
    public static final String NA_GRID = "FN31";

    /** European grid square (UK area). */
    public static final String EU_GRID = "JO01";

    /** Asian grid square (Japan area). */
    public static final String AS_GRID = "PM95";

    /** Oceania grid square (Australia area). */
    public static final String OC_GRID = "QF22";

    /** West coast NA grid square. */
    public static final String NA_WEST_GRID = "CM97";

    private TestConstants() {
        // Prevent instantiation
    }
}
