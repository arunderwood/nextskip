/**
 * Mode Registry - Extensible configuration for supported operating modes.
 *
 * This module implements the Open-Closed Principle: new modes can be added
 * by extending the MODE_REGISTRY array without modifying card logic.
 *
 * When adding a new mode:
 * 1. Add it to the Mode enum in backend (io.nextskip.common.model.Mode)
 * 2. Add configuration here with isSupported: true/false
 * 3. Cards automatically display the new mode
 */

/**
 * Operating modes for amateur radio communications.
 * Matches backend Mode enum when it exists.
 */
export type Mode = 'FT8' | 'FT4' | 'CW' | 'SSB' | 'RTTY' | 'PSK31' | 'JS8';

/**
 * Configuration for a radio operating mode.
 */
export interface ModeConfig {
  /** Mode identifier matching backend Mode enum value */
  mode: Mode;

  /** Whether this mode is fully supported with activity data */
  isSupported: boolean;

  /** Optional override for display name (defaults to mode value) */
  displayName?: string;
}

/**
 * Registry of all modes and their support status.
 *
 * To add a new mode, add an entry here. No other code changes required.
 */
const MODE_REGISTRY: ModeConfig[] = [
  { mode: 'FT8', isSupported: true },
  { mode: 'CW', isSupported: true },
  { mode: 'FT4', isSupported: false },
  { mode: 'SSB', isSupported: false },
  { mode: 'RTTY', isSupported: false },
  { mode: 'PSK31', isSupported: false },
  { mode: 'JS8', isSupported: false },
];

/**
 * Get configuration for a specific mode.
 *
 * @param mode - Mode identifier
 * @returns ModeConfig if found, undefined otherwise
 */
export function getModeConfig(mode: Mode | string | undefined): ModeConfig | undefined {
  if (!mode) return undefined;
  return MODE_REGISTRY.find((config) => config.mode === mode);
}

/**
 * Get all modes that are fully supported with activity data.
 *
 * @returns Array of supported ModeConfig entries
 */
export function getSupportedModes(): ModeConfig[] {
  return MODE_REGISTRY.filter((config) => config.isSupported);
}

/**
 * Get all modes (supported and unsupported).
 *
 * @returns Array of all ModeConfig entries
 */
export function getAllModes(): ModeConfig[] {
  return [...MODE_REGISTRY];
}

/**
 * Check if a mode is supported.
 *
 * @param mode - Mode identifier
 * @returns true if mode is supported, false otherwise
 */
export function isModeSupported(mode: Mode | string | undefined): boolean {
  const config = getModeConfig(mode);
  return config?.isSupported ?? false;
}

/**
 * Get display name for a mode.
 *
 * @param mode - Mode identifier
 * @returns Display name (uses override if set, otherwise mode value)
 */
export function getModeDisplayName(mode: Mode | string | undefined): string {
  if (!mode) return '';
  const config = getModeConfig(mode);
  return config?.displayName ?? mode;
}
