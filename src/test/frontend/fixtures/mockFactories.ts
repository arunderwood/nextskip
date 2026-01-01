/**
 * Mock factory functions for creating test data.
 *
 * These factories create realistic test objects with sensible defaults,
 * allowing overrides for specific test scenarios.
 */
import type Activation from '../mocks/generated/io/nextskip/activations/model/Activation';
import ActivationType from '../mocks/generated/io/nextskip/activations/model/ActivationType';
import type BandCondition from '../mocks/generated/io/nextskip/propagation/model/BandCondition';
import BandConditionRating from '../mocks/generated/io/nextskip/propagation/model/BandConditionRating';
import FrequencyBand from '../mocks/generated/io/nextskip/common/model/FrequencyBand';
import type SolarIndices from '../mocks/generated/io/nextskip/propagation/model/SolarIndices';
import { TEST_CONSTANTS } from '../testConstants';

// =============================================================================
// Park Type (matches ActivationLocation interface for POTA parks)
// =============================================================================

/**
 * Park location for POTA activations.
 */
export interface Park {
  reference: string;
  name: string;
  regionCode?: string;
  countryCode?: string;
  grid?: string;
  latitude?: number;
  longitude?: number;
}

/**
 * Creates a mock Park with sensible defaults.
 *
 * @param overrides - Optional partial Park to override defaults
 * @returns A complete Park object
 */
export function createMockPark(overrides?: Partial<Park>): Park {
  return {
    reference: TEST_CONSTANTS.DEFAULT_PARK_REF,
    name: TEST_CONSTANTS.DEFAULT_PARK_NAME,
    regionCode: TEST_CONSTANTS.DEFAULT_PARK_STATE,
    countryCode: TEST_CONSTANTS.DEFAULT_PARK_COUNTRY,
    grid: TEST_CONSTANTS.DEFAULT_GRID,
    latitude: TEST_CONSTANTS.DEFAULT_LATITUDE,
    longitude: TEST_CONSTANTS.DEFAULT_LONGITUDE,
    ...overrides,
  };
}

// =============================================================================
// Summit Type (matches ActivationLocation interface for SOTA summits)
// =============================================================================

/**
 * Summit location for SOTA activations.
 */
export interface Summit {
  reference: string;
  name: string;
  regionCode?: string;
  associationCode?: string;
}

/**
 * Creates a mock Summit with sensible defaults.
 *
 * @param overrides - Optional partial Summit to override defaults
 * @returns A complete Summit object
 */
export function createMockSummit(overrides?: Partial<Summit>): Summit {
  return {
    reference: TEST_CONSTANTS.DEFAULT_SUMMIT_REF,
    name: TEST_CONSTANTS.DEFAULT_SUMMIT_NAME,
    regionCode: 'WA',
    associationCode: 'W7W',
    ...overrides,
  };
}

// =============================================================================
// Activation Factory
// =============================================================================

/**
 * Creates a mock Activation with sensible defaults.
 *
 * @param overrides - Optional partial Activation to override defaults
 * @returns A complete Activation object
 */
export function createMockActivation(overrides?: Partial<Activation>): Activation {
  return {
    spotId: 'test-spot-1',
    activatorCallsign: TEST_CONSTANTS.DEFAULT_CALLSIGN,
    type: ActivationType.POTA,
    frequency: TEST_CONSTANTS.DEFAULT_FREQUENCY,
    mode: TEST_CONSTANTS.DEFAULT_MODE,
    spottedAt: new Date().toISOString(),
    qsoCount: TEST_CONSTANTS.DEFAULT_QSO_COUNT,
    source: 'Test Source',
    location: createMockPark(),
    score: 100,
    favorable: true,
    ...overrides,
  };
}

/**
 * Creates a mock POTA Activation with sensible defaults.
 *
 * @param overrides - Optional partial Activation to override defaults
 * @returns A complete POTA Activation object
 */
export function createMockPotaActivation(overrides?: Partial<Activation>): Activation {
  return createMockActivation({
    type: ActivationType.POTA,
    source: 'POTA API',
    location: createMockPark(),
    ...overrides,
  });
}

/**
 * Creates a mock SOTA Activation with sensible defaults.
 *
 * @param overrides - Optional partial Activation to override defaults
 * @returns A complete SOTA Activation object
 */
export function createMockSotaActivation(overrides?: Partial<Activation>): Activation {
  return createMockActivation({
    type: ActivationType.SOTA,
    source: 'SOTA API',
    mode: 'CW',
    frequency: 7200.0,
    location: createMockSummit(),
    ...overrides,
  });
}

/**
 * Creates a stale activation (spotted 2 hours ago).
 *
 * @param overrides - Optional partial Activation to override defaults
 * @returns An Activation that was spotted 2 hours ago
 */
export function createStaleActivation(overrides?: Partial<Activation>): Activation {
  const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
  return createMockActivation({
    spottedAt: twoHoursAgo.toISOString(),
    score: 0,
    favorable: false,
    ...overrides,
  });
}

// =============================================================================
// SolarIndices Factory
// =============================================================================

/**
 * Creates mock SolarIndices with sensible defaults (good conditions).
 *
 * @param overrides - Optional partial SolarIndices to override defaults
 * @returns A complete SolarIndices object
 */
export function createMockSolarIndices(overrides?: Partial<SolarIndices>): SolarIndices {
  return {
    solarFluxIndex: TEST_CONSTANTS.DEFAULT_SFI,
    aIndex: TEST_CONSTANTS.DEFAULT_A_INDEX,
    kIndex: TEST_CONSTANTS.DEFAULT_K_INDEX,
    sunspotNumber: TEST_CONSTANTS.DEFAULT_SUNSPOT_NUMBER,
    timestamp: new Date().toISOString(),
    source: 'Test Source',
    geomagneticActivity: 'Quiet',
    solarFluxLevel: 'Moderate',
    score: 70,
    favorable: true,
    ...overrides,
  };
}

/**
 * Creates mock SolarIndices representing excellent conditions.
 *
 * @returns SolarIndices with high SFI and low K-index
 */
export function createExcellentSolarIndices(): SolarIndices {
  return createMockSolarIndices({
    solarFluxIndex: 180.0,
    aIndex: 5,
    kIndex: 1,
    sunspotNumber: 100,
    geomagneticActivity: 'Quiet',
    solarFluxLevel: 'High',
    score: 95,
    favorable: true,
  });
}

/**
 * Creates mock SolarIndices representing poor conditions.
 *
 * @returns SolarIndices with low SFI and high K-index
 */
export function createPoorSolarIndices(): SolarIndices {
  return createMockSolarIndices({
    solarFluxIndex: 65.0,
    aIndex: 30,
    kIndex: 6,
    sunspotNumber: 10,
    geomagneticActivity: 'Active',
    solarFluxLevel: 'Very Low',
    score: 20,
    favorable: false,
  });
}

// =============================================================================
// BandCondition Factory
// =============================================================================

/**
 * Creates a mock BandCondition with sensible defaults.
 *
 * @param overrides - Optional partial BandCondition to override defaults
 * @returns A complete BandCondition object
 */
export function createMockBandCondition(overrides?: Partial<BandCondition>): BandCondition {
  return {
    band: FrequencyBand.BAND_20M,
    rating: BandConditionRating.GOOD,
    confidence: 1.0,
    notes: undefined,
    score: 100,
    favorable: true,
    ...overrides,
  };
}

/**
 * Creates a GOOD BandCondition for the specified band.
 *
 * @param band - The frequency band
 * @returns A BandCondition with GOOD rating
 */
export function createGoodBandCondition(band: FrequencyBand): BandCondition {
  return createMockBandCondition({
    band,
    rating: BandConditionRating.GOOD,
    score: 100,
    favorable: true,
  });
}

/**
 * Creates a FAIR BandCondition for the specified band.
 *
 * @param band - The frequency band
 * @returns A BandCondition with FAIR rating
 */
export function createFairBandCondition(band: FrequencyBand): BandCondition {
  return createMockBandCondition({
    band,
    rating: BandConditionRating.FAIR,
    score: 60,
    favorable: false,
  });
}

/**
 * Creates a POOR BandCondition for the specified band.
 *
 * @param band - The frequency band
 * @returns A BandCondition with POOR rating
 */
export function createPoorBandCondition(band: FrequencyBand): BandCondition {
  return createMockBandCondition({
    band,
    rating: BandConditionRating.POOR,
    score: 20,
    favorable: false,
  });
}

// =============================================================================
// ActivityCardConfig Factory (for grid and card component testing)
// =============================================================================

// Import the actual type from main frontend code
import type { ActivityCardConfig } from 'Frontend/types/activity';

/**
 * Creates a mock ActivityCardConfig with sensible defaults.
 *
 * @param overrides - Optional partial ActivityCardConfig to override defaults
 * @returns A complete ActivityCardConfig object
 */
export function createMockActivityCardConfig(overrides?: Partial<ActivityCardConfig>): ActivityCardConfig {
  return {
    id: 'test-card',
    type: 'solar-indices',
    size: '1x1',
    priority: 50,
    hotness: 'neutral',
    ...overrides,
  };
}

// Re-export the type for convenience
export type { ActivityCardConfig };

// =============================================================================
// Card Factory (for priority calculation testing)
// =============================================================================

/**
 * Card input for priority calculation testing.
 */
export interface CardInput {
  id: string;
  title: string;
  favorable: boolean;
  score: number;
  rating?: 'GOOD' | 'FAIR' | 'POOR' | 'UNKNOWN';
  lastUpdated?: Date;
}

/**
 * Creates a mock card input for testing priority calculations.
 *
 * @param overrides - Optional partial CardInput to override defaults
 * @returns A complete CardInput object
 */
export function createMockCard(overrides?: Partial<CardInput>): CardInput {
  return {
    id: 'test-card-1',
    title: 'Test Card',
    favorable: true,
    score: 75,
    rating: 'GOOD',
    lastUpdated: new Date(),
    ...overrides,
  };
}

/**
 * Creates a high-priority "hot" card.
 *
 * @returns A CardInput with maximum priority values
 */
export function createHotCard(): CardInput {
  return createMockCard({
    id: 'hot-card',
    title: 'Hot Card',
    favorable: true,
    score: 100,
    rating: 'GOOD',
    lastUpdated: new Date(),
  });
}

/**
 * Creates a low-priority "cool" card.
 *
 * @returns A CardInput with minimum priority values
 */
export function createCoolCard(): CardInput {
  return createMockCard({
    id: 'cool-card',
    title: 'Cool Card',
    favorable: false,
    score: 10,
    rating: 'POOR',
    lastUpdated: new Date(Date.now() - 24 * 60 * 60 * 1000),
  });
}

// =============================================================================
// Re-export types and enums for convenience
// =============================================================================

export { ActivationType, BandConditionRating, FrequencyBand };
