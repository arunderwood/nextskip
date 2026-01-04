/**
 * Unit tests for useDashboardCards hook
 *
 * Tests the orchestration of PropagationResponse data into ActivityCardConfig objects.
 * Priority calculation and hotness logic are tested in usePriorityCalculation.test.ts.
 */

import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useDashboardCards } from 'Frontend/hooks/useDashboardCards';
import type { DashboardData } from 'Frontend/components/cards/types';
import type PropagationResponse from 'Frontend/generated/io/nextskip/propagation/api/PropagationResponse';
import type SolarIndices from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import FrequencyBand from 'Frontend/generated/io/nextskip/common/model/FrequencyBand';
import BandConditionRating from 'Frontend/generated/io/nextskip/propagation/model/BandConditionRating';

// Import card registrations to ensure they're loaded for tests
import 'Frontend/components/cards/propagation';

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

  it('should create individual card for each band condition', () => {
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse({
        solarIndices: undefined,
        bandConditions: [
          createMockBandCondition({ band: FrequencyBand.BAND_20M }),
          createMockBandCondition({ band: FrequencyBand.BAND_40M }),
        ],
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    // 1 solar (loading) + 2 individual band cards
    expect(result.current).toHaveLength(3);
    expect(result.current.map((c) => c.id)).toEqual(
      expect.arrayContaining(['solar-indices', 'band-BAND_20M', 'band-BAND_40M']),
    );
  });

  it('should use band-condition type and 1x1 size for band cards', () => {
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse({
        solarIndices: undefined,
        bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M })],
      }),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    const bandCard = result.current.find((c) => c.id === 'band-BAND_20M');
    expect(bandCard).toMatchObject({
      type: 'band-condition',
      size: '1x1',
    });
  });

  it('should use individual band score as priority', () => {
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

    const band20m = result.current.find((c) => c.id === 'band-BAND_20M');
    const band40m = result.current.find((c) => c.id === 'band-BAND_40M');
    expect(band20m?.priority).toBe(80);
    expect(band40m?.priority).toBe(60);
  });

  it('should return solar and band cards when all data present', () => {
    const dashboardData: DashboardData = {
      propagation: createMockPropagationResponse(),
    };
    const { result } = renderHook(() => useDashboardCards(dashboardData));

    // 1 solar + 3 individual band cards from default mock
    expect(result.current).toHaveLength(4);
    expect(result.current.find((c) => c.id === 'solar-indices')).toBeDefined();
    expect(result.current.find((c) => c.id === 'band-BAND_80M')).toBeDefined();
    expect(result.current.find((c) => c.id === 'band-BAND_40M')).toBeDefined();
    expect(result.current.find((c) => c.id === 'band-BAND_20M')).toBeDefined();
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
