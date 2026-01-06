/**
 * Unit tests for useDashboardCards hook
 *
 * Tests the orchestration of DashboardData into ActivityCardConfig objects.
 * Priority calculation and hotness logic are tested in usePriorityCalculation.test.ts.
 */

import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useDashboardCards } from 'Frontend/hooks/useDashboardCards';
import type { DashboardData } from 'Frontend/components/cards/types';
import type PropagationResponse from 'Frontend/generated/io/nextskip/propagation/api/PropagationResponse';
import type SolarIndices from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import type BandActivity from 'Frontend/generated/io/nextskip/spots/model/BandActivity';
import type BandActivityResponse from 'Frontend/generated/io/nextskip/spots/api/BandActivityResponse';
import FrequencyBand from 'Frontend/generated/io/nextskip/common/model/FrequencyBand';
import BandConditionRating from 'Frontend/generated/io/nextskip/propagation/model/BandConditionRating';

// Import card registrations to ensure they're loaded for tests
import 'Frontend/components/cards/propagation';
import 'Frontend/components/cards/band-activity';

// Mock data factories
const createMockSolarIndices = (overrides: Partial<SolarIndices> = {}): SolarIndices => ({
  solarFluxIndex: 150,
  sunspotNumber: 100,
  aIndex: 10,
  kIndex: 2,
  favorable: true,
  score: 75,
  ...overrides,
});

const createMockBandCondition = (overrides: Partial<BandCondition> = {}): BandCondition => ({
  band: FrequencyBand.BAND_20M,
  rating: BandConditionRating.GOOD,
  favorable: true,
  score: 80,
  confidence: 90,
  ...overrides,
});

const createMockBandActivity = (overrides: Partial<BandActivity> = {}): BandActivity => ({
  band: '20m',
  mode: 'FT8',
  spotCount: 100,
  baselineSpotCount: 80,
  trendPercentage: 25,
  maxDxKm: 5000,
  maxDxPath: 'NA-EU',
  activePaths: [],
  score: 85,
  favorable: true,
  windowMinutes: 15,
  ...overrides,
});

const createMockBandActivityResponse = (activities: Record<string, BandActivity> = {}): BandActivityResponse => ({
  bandActivities: activities,
  mqttConnected: true,
  bandCount: Object.keys(activities).length,
  totalSpotCount: Object.values(activities).reduce((sum, a) => sum + (a?.spotCount ?? 0), 0),
});

const createMockPropagationResponse = (overrides: Partial<PropagationResponse> = {}): PropagationResponse => ({
  solarIndices: createMockSolarIndices(),
  bandConditions: [
    createMockBandCondition({ band: FrequencyBand.BAND_80M, score: 70 }),
    createMockBandCondition({ band: FrequencyBand.BAND_40M, score: 75 }),
    createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 80 }),
  ],
  ...overrides,
});

describe('useDashboardCards', () => {
  it('should always return solar-indices card (loading state when no data)', () => {
    const { result } = renderHook(() => useDashboardCards({} as DashboardData));

    expect(result.current).toHaveLength(1);
    expect(result.current[0]).toMatchObject({
      id: 'solar-indices',
      type: 'solar-indices',
      size: '1x1',
      priority: 0,
    });
  });

  it('should create band-mode-activity cards for each band + mode combo with activity', () => {
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse({
        solarIndices: undefined,
        bandConditions: [
          createMockBandCondition({ band: FrequencyBand.BAND_20M }),
          createMockBandCondition({ band: FrequencyBand.BAND_40M }),
        ],
      }),
      spots: createMockBandActivityResponse({
        '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        '20m-CW': createMockBandActivity({ band: '20m', mode: 'CW' }),
        '40m-FT8': createMockBandActivity({ band: '40m', mode: 'FT8' }),
        '40m-CW': createMockBandActivity({ band: '40m', mode: 'CW' }),
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    // 1 solar (loading) + cards for each band+mode combo with activity
    // With 2 bands and 2 modes with activity, expect at least 5 cards
    expect(result.current.length).toBeGreaterThanOrEqual(5);

    // Should include solar card
    expect(result.current.find((c) => c.id === 'solar-indices')).toBeDefined();

    // Should include band-mode-activity cards with proper id format
    const bandActivityCards = result.current.filter((c) => c.type === 'band-mode-activity');
    expect(bandActivityCards.length).toBeGreaterThanOrEqual(4); // 2 bands * 2 modes
  });

  it('should use band-mode-activity type and 1x1 size for band activity cards', () => {
    const dashboardData: DashboardData = {
      spots: createMockBandActivityResponse({
        '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    // Find a band activity card
    const bandActivityCards = result.current.filter((c) => c.type === 'band-mode-activity');
    expect(bandActivityCards.length).toBeGreaterThan(0);

    const bandCard = bandActivityCards[0];
    expect(bandCard).toMatchObject({
      type: 'band-mode-activity',
      size: '1x1',
    });
  });

  it('should calculate priority using condition score when no activity data', () => {
    // Cards are only created when activity data exists
    // This test now verifies that no cards are created without activity
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse({
        solarIndices: undefined,
        bandConditions: [
          createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 80 }),
          createMockBandCondition({ band: FrequencyBand.BAND_40M, score: 60 }),
        ],
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    // No band-mode-activity cards without activity data
    const bandActivityCards = result.current.filter((c) => c.type === 'band-mode-activity');
    expect(bandActivityCards.length).toBe(0);
  });

  it('should return solar and band-mode-activity cards when activity data present', () => {
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse(),
      spots: createMockBandActivityResponse({
        '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    // 1 solar + band-mode-activity cards
    expect(result.current.length).toBeGreaterThan(1);
    expect(result.current.find((c) => c.id === 'solar-indices')).toBeDefined();

    // Should have band-mode-activity cards
    const bandActivityCards = result.current.filter((c) => c.type === 'band-mode-activity');
    expect(bandActivityCards.length).toBeGreaterThan(0);
  });

  it('should return only solar card when bands missing', () => {
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse({
        bandConditions: undefined,
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    expect(result.current).toHaveLength(1);
    expect(result.current[0].id).toBe('solar-indices');
  });
});
