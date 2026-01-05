/**
 * Spotter Source Abstraction
 *
 * Defines interfaces for spotter/logging network data sources.
 * Implements Dependency Inversion - cards depend on these interfaces,
 * not concrete endpoints.
 *
 * Future spotter networks (RBN, DX Cluster) implement these interfaces.
 */

import type { Mode } from 'Frontend/config/modeRegistry';

/**
 * Aggregated activity data for a specific band+mode combination.
 *
 * This interface abstracts the backend BandActivity model for frontend use,
 * allowing multiple spotter sources to provide compatible data.
 */
export interface BandModeActivity {
  /** Band identifier (e.g., "20m", "40m") */
  band: string;

  /** Operating mode (e.g., "FT8", "CW") */
  mode: Mode | string;

  /** Number of spots in the current time window */
  spotCount: number;

  /** Rolling average of spots in prior windows */
  baselineSpotCount: number;

  /** Percentage change vs baseline (-100 to +infinity) */
  trendPercentage: number;

  /** Maximum DX distance in km (optional) */
  maxDxKm?: number;

  /** Description of max DX path (e.g., "JA1ABC → W6XYZ") (optional) */
  maxDxPath?: string;

  /** Active continent paths (e.g., ["NA_EU", "NA_AS"]) */
  activePaths: string[];

  /** Activity score 0-100 */
  score: number;

  /** Duration of the aggregation window in minutes */
  windowMinutes?: number;
}

/**
 * Data from a spotter network source.
 *
 * Each spotter network (PSKReporter, RBN, etc.) provides data in this format.
 */
export interface SpotterSourceData {
  /** Map of band+mode key to activity data (e.g., "20m-FT8" → BandModeActivity) */
  activities: Map<string, BandModeActivity>;

  /** Name of the spotter network (e.g., "PSKReporter", "RBN") */
  sourceName: string;

  /** Whether the connection to the spotter network is healthy */
  isConnected: boolean;
}

/**
 * Create a lookup key for band+mode combination.
 *
 * @param band - Band identifier (e.g., "20m")
 * @param mode - Mode identifier (e.g., "FT8")
 * @returns Lookup key (e.g., "20m-FT8")
 */
export function createBandModeKey(band: string, mode: string): string {
  return `${band}-${mode}`;
}

/**
 * Parse a band+mode key into its components.
 *
 * @param key - Lookup key (e.g., "20m-FT8")
 * @returns Object with band and mode, or undefined if invalid
 */
export function parseBandModeKey(key: string): { band: string; mode: string } | undefined {
  const parts = key.split('-');
  if (parts.length !== 2) return undefined;
  return { band: parts[0], mode: parts[1] };
}
