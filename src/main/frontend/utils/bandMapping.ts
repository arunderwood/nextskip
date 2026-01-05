/**
 * Band Mapping Utilities
 *
 * Provides conversion between FrequencyBand enum values (e.g., "BAND_20M")
 * and string band names (e.g., "20m") used by the spots module.
 *
 * This handles the mismatch between the propagation module's FrequencyBand enum
 * and the spots module's string-based band identifiers.
 */

/**
 * Map from FrequencyBand enum value to string band name.
 */
export const BAND_ENUM_TO_STRING: Record<string, string> = {
  BAND_160M: '160m',
  BAND_80M: '80m',
  BAND_60M: '60m',
  BAND_40M: '40m',
  BAND_30M: '30m',
  BAND_20M: '20m',
  BAND_17M: '17m',
  BAND_15M: '15m',
  BAND_12M: '12m',
  BAND_10M: '10m',
  BAND_6M: '6m',
  BAND_2M: '2m',
};

/**
 * Map from string band name to FrequencyBand enum value.
 */
export const BAND_STRING_TO_ENUM: Record<string, string> = Object.fromEntries(
  Object.entries(BAND_ENUM_TO_STRING).map(([enumVal, strVal]) => [strVal.toLowerCase(), enumVal]),
);

/**
 * Ordered list of bands from lowest to highest frequency.
 * Used for consistent sorting across the UI.
 */
export const BAND_ORDER: string[] = ['160m', '80m', '60m', '40m', '30m', '20m', '17m', '15m', '12m', '10m', '6m', '2m'];

/**
 * Normalize a band identifier to string format (e.g., "20m").
 *
 * Handles both enum values (BAND_20M) and string values (20m, 20M).
 *
 * @param band - Band identifier in any format
 * @returns Normalized string band name (e.g., "20m"), or original input if unknown
 */
export function normalizeBandToString(band: string | undefined | null): string {
  if (!band) return '';

  // Already in string format (e.g., "20m")
  const lowerBand = band.toLowerCase();
  if (BAND_STRING_TO_ENUM[lowerBand]) {
    return lowerBand;
  }

  // Enum format (e.g., "BAND_20M")
  const upperBand = band.toUpperCase();
  if (BAND_ENUM_TO_STRING[upperBand]) {
    return BAND_ENUM_TO_STRING[upperBand];
  }

  // Try stripping "BAND_" prefix if present
  if (upperBand.startsWith('BAND_')) {
    const stripped = upperBand.slice(5).toLowerCase();
    return stripped;
  }

  // Return original lowercase as fallback
  return lowerBand;
}

/**
 * Normalize a band identifier to enum format (e.g., "BAND_20M").
 *
 * Handles both string values (20m) and enum values (BAND_20M).
 *
 * @param band - Band identifier in any format
 * @returns Normalized enum value (e.g., "BAND_20M"), or constructed enum if unknown
 */
export function normalizeBandToEnum(band: string | undefined | null): string {
  if (!band) return '';

  // Already in enum format (e.g., "BAND_20M")
  const upperBand = band.toUpperCase();
  if (BAND_ENUM_TO_STRING[upperBand]) {
    return upperBand;
  }

  // String format (e.g., "20m")
  const lowerBand = band.toLowerCase();
  if (BAND_STRING_TO_ENUM[lowerBand]) {
    return BAND_STRING_TO_ENUM[lowerBand];
  }

  // Construct enum format as fallback
  return `BAND_${upperBand}`;
}

/**
 * Get the sort index for a band (lower = lower frequency).
 *
 * @param band - Band identifier in any format
 * @returns Sort index (0 = 160m, 11 = 2m), or -1 if unknown
 */
export function getBandSortIndex(band: string | undefined | null): number {
  if (!band) return -1;
  const normalized = normalizeBandToString(band);
  return BAND_ORDER.indexOf(normalized);
}

/**
 * Compare two bands by frequency (lower frequency first).
 *
 * @param bandA - First band identifier
 * @param bandB - Second band identifier
 * @returns Negative if A < B, positive if A > B, zero if equal
 */
export function compareBands(bandA: string, bandB: string): number {
  return getBandSortIndex(bandA) - getBandSortIndex(bandB);
}

/**
 * Check if two band identifiers refer to the same band.
 *
 * @param bandA - First band identifier
 * @param bandB - Second band identifier
 * @returns true if both refer to the same band
 */
export function bandsMatch(bandA: string | undefined | null, bandB: string | undefined | null): boolean {
  if (!bandA || !bandB) return false;
  return normalizeBandToString(bandA) === normalizeBandToString(bandB);
}
