/**
 * Band-specific DX distance thresholds for scoring and help text.
 *
 * Different bands have vastly different propagation characteristics:
 * - 160m: 3,000 km is exceptional (requires night skip, low noise)
 * - 20m: 15,000 km is needed for excellent score (workhorse DX band)
 * - 6m: 5,000 km is legendary F2 propagation; 2,000 km sporadic-E is exciting
 *
 * KEEP IN SYNC with FrequencyBand.java DxThresholds
 * @see src/main/java/io/nextskip/common/model/FrequencyBand.java
 */
export const bandDxThresholds = [
  { band: '160m', excellentKm: 3_000, goodKm: 1_500, moderateKm: 500, description: 'Difficult; night skip required' },
  { band: '80m', excellentKm: 5_000, goodKm: 2_500, moderateKm: 1_000, description: 'Regional; some DX at night' },
  {
    band: '60m',
    excellentKm: 6_000,
    goodKm: 3_000,
    moderateKm: 1_500,
    description: 'Secondary allocation; variable propagation',
  },
  { band: '40m', excellentKm: 7_000, goodKm: 4_000, moderateKm: 2_000, description: 'Day/night transitions' },
  { band: '30m', excellentKm: 8_000, goodKm: 5_000, moderateKm: 2_500, description: 'WARC; reliable propagation' },
  { band: '20m', excellentKm: 15_000, goodKm: 10_000, moderateKm: 5_000, description: 'Workhorse DX band' },
  { band: '17m', excellentKm: 12_000, goodKm: 8_000, moderateKm: 4_000, description: 'WARC; solar-dependent' },
  {
    band: '15m',
    excellentKm: 14_000,
    goodKm: 9_000,
    moderateKm: 4_500,
    description: 'Solar-dependent; excellent when open',
  },
  { band: '12m', excellentKm: 13_000, goodKm: 8_500, moderateKm: 4_000, description: 'WARC; similar to 10m/15m' },
  { band: '10m', excellentKm: 12_000, goodKm: 7_000, moderateKm: 3_000, description: 'Magic when open' },
  { band: '6m', excellentKm: 5_000, goodKm: 2_000, moderateKm: 500, description: 'Sporadic-E; rare F2' },
  { band: '2m', excellentKm: 2_000, goodKm: 500, moderateKm: 100, description: 'Tropo/EME' },
] as const;

export type BandDxThreshold = (typeof bandDxThresholds)[number];

/**
 * Base threshold shape without band-specific literal types.
 */
export interface DxThresholdBase {
  excellentKm: number;
  goodKm: number;
  moderateKm: number;
  description: string;
}

/**
 * Get DX thresholds for a specific band.
 *
 * @param band - Band name (e.g., '20m', '40m')
 * @returns Threshold data for the band, or undefined if not found
 */
export function getThresholdsForBand(band: string): BandDxThreshold | undefined {
  return bandDxThresholds.find((t) => t.band === band);
}

/**
 * Default thresholds for unknown bands.
 * Uses conservative values similar to 40m.
 */
export const defaultDxThresholds: DxThresholdBase = {
  excellentKm: 7_000,
  goodKm: 4_000,
  moderateKm: 2_000,
  description: 'Moderate propagation',
};
