/**
 * Band Conditions Utilities
 *
 * Shared utilities for working with HF band propagation conditions.
 * Used by BandConditionsTable and BandConditionsContent components.
 */

import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';

/**
 * Get CSS class name for a rating
 */
export function getRatingClass(rating: string): string {
  switch (rating?.toUpperCase()) {
    case 'GOOD':
      return 'rating-good';
    case 'FAIR':
      return 'rating-fair';
    case 'POOR':
      return 'rating-poor';
    default:
      return 'rating-unknown';
  }
}

/**
 * Get descriptive text for a band
 */
export function getBandDescription(band: string): string {
  const descriptions: Record<string, string> = {
    BAND_160M: 'Long distance, nighttime',
    BAND_80M: 'Regional to DX, night',
    BAND_40M: 'All-around workhorse',
    BAND_30M: 'Digital modes, quiet',
    BAND_20M: 'DX powerhouse',
    BAND_17M: 'Underutilized gem',
    BAND_15M: 'DX when conditions support',
    BAND_12M: 'Daytime DX',
    BAND_10M: 'Solar cycle dependent',
    BAND_6M: 'Magic band',
  };
  return descriptions[band] || '';
}

/**
 * Format band name for display (removes "BAND_" prefix)
 */
export function formatBandName(band: string): string {
  return band?.replace('BAND_', '') || 'Unknown';
}

/**
 * Band order for sorting (highest frequency to lowest)
 */
export const BAND_ORDER = [
  'BAND_160M',
  'BAND_80M',
  'BAND_40M',
  'BAND_30M',
  'BAND_20M',
  'BAND_17M',
  'BAND_15M',
  'BAND_12M',
  'BAND_10M',
  'BAND_6M',
];

/**
 * Sort band conditions by frequency order
 */
export function sortBandConditions(conditions: BandCondition[]): BandCondition[] {
  return [...conditions].sort((a, b) => {
    return BAND_ORDER.indexOf(a.band || '') - BAND_ORDER.indexOf(b.band || '');
  });
}
