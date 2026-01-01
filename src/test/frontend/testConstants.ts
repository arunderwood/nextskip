/**
 * Shared test constants for frontend tests.
 *
 * Centralizes commonly used test values to reduce duplication and magic numbers.
 * All values should be realistic examples that match production data formats.
 */
export const TEST_CONSTANTS = {
  // ==========================================================================
  // Callsigns
  // ==========================================================================

  /** Default amateur radio callsign for tests */
  DEFAULT_CALLSIGN: 'W1ABC',

  /** Secondary callsign for multi-activation tests */
  SECONDARY_CALLSIGN: 'K2DEF',

  /** Portable callsign suffix pattern */
  PORTABLE_CALLSIGN: 'W3XYZ/P',

  // ==========================================================================
  // POTA (Parks on the Air)
  // ==========================================================================

  /** Default POTA park reference code */
  DEFAULT_PARK_REF: 'K-1234',

  /** Default POTA park name */
  DEFAULT_PARK_NAME: 'Test Park',

  /** Default POTA state/region code */
  DEFAULT_PARK_STATE: 'CO',

  /** Default POTA country code */
  DEFAULT_PARK_COUNTRY: 'US',

  // ==========================================================================
  // SOTA (Summits on the Air)
  // ==========================================================================

  /** Default SOTA summit reference code */
  DEFAULT_SUMMIT_REF: 'W7W/LC-001',

  /** Default SOTA summit name */
  DEFAULT_SUMMIT_NAME: 'Test Summit',

  // ==========================================================================
  // Location and Coordinates
  // ==========================================================================

  /** Default latitude in decimal degrees (Boston area) */
  DEFAULT_LATITUDE: 42.5,

  /** Default longitude in decimal degrees (Boston area) */
  DEFAULT_LONGITUDE: -71.3,

  /** Default Maidenhead grid square */
  DEFAULT_GRID: 'FN42',

  // ==========================================================================
  // Radio Parameters
  // ==========================================================================

  /** Default operating frequency in kHz (20m band) */
  DEFAULT_FREQUENCY: 14250.0,

  /** Default operating mode */
  DEFAULT_MODE: 'SSB',

  /** Default QSO count for active stations */
  DEFAULT_QSO_COUNT: 10,

  // ==========================================================================
  // External URLs
  // ==========================================================================

  /** QRZ.com base URL for callsign lookups */
  QRZ_BASE_URL: 'https://www.qrz.com/db/',

  /** POTA spots API base URL */
  POTA_API_URL: 'https://api.pota.app/spot/activator',

  // ==========================================================================
  // Scoring Thresholds
  // ==========================================================================

  /** Hotness level thresholds for activity cards */
  HOTNESS_THRESHOLDS: {
    HOT: 70,
    WARM: 45,
    NEUTRAL: 20,
  },

  // ==========================================================================
  // Priority Calculation Weights
  // ==========================================================================

  /** Weights for priority calculation algorithm (must sum to 100) */
  PRIORITY_WEIGHTS: {
    /** Weight for favorable flag (40%) */
    FAVORABLE: 40,
    /** Weight for numeric score (35%) */
    SCORE: 35,
    /** Weight for rating enum (20%) */
    RATING: 20,
    /** Weight for recency/time decay (5%) */
    RECENCY: 5,
  },

  // ==========================================================================
  // Solar Indices
  // ==========================================================================

  /** Default Solar Flux Index (good conditions) */
  DEFAULT_SFI: 100.0,

  /** Default planetary A-index */
  DEFAULT_A_INDEX: 10,

  /** Default planetary K-index */
  DEFAULT_K_INDEX: 2,

  /** Default sunspot number */
  DEFAULT_SUNSPOT_NUMBER: 50,

  // ==========================================================================
  // Time Constants (milliseconds)
  // ==========================================================================

  /** Duration in ms for "fresh" activations (1 hour) */
  FRESH_MS: 60 * 60 * 1000,

  /** Duration in ms for "stale" activations (24 hours) */
  STALE_MS: 24 * 60 * 60 * 1000,
} as const;

/**
 * Type for the TEST_CONSTANTS object to enable type inference.
 */
export type TestConstantsType = typeof TEST_CONSTANTS;
