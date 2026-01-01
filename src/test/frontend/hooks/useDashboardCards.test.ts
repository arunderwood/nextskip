/**
 * Unit tests for useDashboardCards hook
 *
 * Tests the orchestration of PropagationResponse data into ActivityCardConfig
 * objects with calculated priorities and hotness levels.
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
  describe('with undefined data', () => {
    it('should return solar-indices card with loading state', () => {
      const { result } = renderHook(() => useDashboardCards({} as DashboardData));
      // Solar indices card always shows (with loading state when no data)
      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      expect(result.current[0].priority).toBe(0); // Low priority when no data
    });
  });

  describe('solarInput calculation', () => {
    it('should return solar-indices with loading state when solarIndices undefined', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Solar indices card always shows (with loading state when no data)
      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      expect(result.current[0].priority).toBe(0);
      expect(result.current[0].hotness).toBe('neutral');
    });

    it('should return GOOD rating when solarFluxIndex >= 150', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({ solarFluxIndex: 150 }),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      // Priority should be high for GOOD rating (favorable=true, score=150 clamped to 100, rating=GOOD)
      expect(result.current[0].priority).toBeGreaterThanOrEqual(70);
    });

    it('should return FAIR rating when solarFluxIndex >= 100 and < 150', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({ solarFluxIndex: 125 }),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      // Should have moderate priority (favorable + score + FAIR rating)
      expect(result.current[0].priority).toBeGreaterThan(40);
      expect(result.current[0].priority).toBeLessThan(90);
    });

    it('should return POOR rating when solarFluxIndex < 100', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({ solarFluxIndex: 80, favorable: false }),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      // Low priority (favorable=false, low score, POOR rating)
      expect(result.current[0].priority).toBeLessThan(40);
    });

    it('should return UNKNOWN rating when solarFluxIndex undefined', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({ solarFluxIndex: undefined }),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      // Should have some priority from favorable flag if true
      expect(result.current[0].priority).toBeGreaterThanOrEqual(0);
    });

    it('should pass favorable flag from backend', () => {
      const favorableData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({ favorable: true, solarFluxIndex: 100 }),
          bandConditions: undefined,
        }),
      };
      const unfavorableData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({ favorable: false, solarFluxIndex: 100 }),
          bandConditions: undefined,
        }),
      };

      const { result: favorableResult } = renderHook(() => useDashboardCards(favorableData));
      const { result: unfavorableResult } = renderHook(() => useDashboardCards(unfavorableData));

      // Favorable flag adds 40 points to priority
      expect(favorableResult.current[0].priority).toBeGreaterThan(unfavorableResult.current[0].priority);
      expect(favorableResult.current[0].priority - unfavorableResult.current[0].priority).toBeGreaterThanOrEqual(35); // Should be ~40 points difference
    });
  });

  describe('bandInput calculation', () => {
    it('should return solar-indices with loading state when bandConditions empty', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Solar indices card always shows (with loading state when no data)
      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
    });

    it('should create individual card for each band condition plus solar loading', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 60 }),
            createMockBandCondition({ band: FrequencyBand.BAND_40M, score: 80 }),
            createMockBandCondition({ band: FrequencyBand.BAND_10M, score: 100 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Should create 4 cards: 1 solar (loading) + 3 individual band cards
      expect(result.current).toHaveLength(4);
      expect(result.current.map((c) => c.id)).toContain('solar-indices');
      expect(result.current.map((c) => c.id)).toContain('band-BAND_20M');
      expect(result.current.map((c) => c.id)).toContain('band-BAND_40M');
      expect(result.current.map((c) => c.id)).toContain('band-BAND_10M');
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

      // Each card should use its individual band's score as priority
      const band20m = result.current.find((c) => c.id === 'band-BAND_20M');
      const band40m = result.current.find((c) => c.id === 'band-BAND_40M');

      expect(band20m?.priority).toBe(80);
      expect(band40m?.priority).toBe(60);
    });

    it('should assign hotness based on individual band score', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 85 }), // hot (>70)
            createMockBandCondition({ band: FrequencyBand.BAND_40M, score: 50 }), // warm (45-69)
            createMockBandCondition({ band: FrequencyBand.BAND_10M, score: 15 }), // cool (<20)
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      const band20m = result.current.find((c) => c.id === 'band-BAND_20M');
      const band40m = result.current.find((c) => c.id === 'band-BAND_40M');
      const band10m = result.current.find((c) => c.id === 'band-BAND_10M');

      expect(band20m?.hotness).toBe('hot');
      expect(band40m?.hotness).toBe('warm');
      expect(band10m?.hotness).toBe('cool');
    });

    it('should handle undefined score as 0 priority', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M, score: undefined })],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // 1 solar (loading) + 1 band card
      expect(result.current).toHaveLength(2);
      const bandCard = result.current.find((c) => c.id === 'band-BAND_20M');
      expect(bandCard?.priority).toBe(0);
    });

    it('should use band-condition type for all individual band cards', () => {
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

      const bandCards = result.current.filter((c) => c.id.startsWith('band-'));
      expect(bandCards.every((c) => c.type === 'band-condition')).toBe(true);
    });

    it('should use standard size for individual band cards', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M })],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current[0].size).toBe('1x1');
    });
  });

  describe('solarIndicesConfig', () => {
    it('should return solar-indices with loading state when no solarIndices data', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M })],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Solar indices always shows + band card
      expect(result.current).toHaveLength(2);
      expect(result.current.find((c) => c.id === 'solar-indices')).toBeDefined();
      expect(result.current.find((c) => c.id === 'band-BAND_20M')).toBeDefined();
    });

    it('should have correct id, type, size', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices(),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0]).toMatchObject({
        id: 'solar-indices',
        type: 'solar-indices',
        size: '1x1',
      });
    });

    it('should calculate priority correctly', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({
            favorable: true,
            solarFluxIndex: 150,
          }),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      // favorable(40) + score clamped to 100(35) + GOOD rating(20) = 95
      expect(result.current[0].priority).toBeGreaterThanOrEqual(90);
    });

    it('should calculate hotness correctly', () => {
      const hotData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({
            favorable: true,
            solarFluxIndex: 150,
          }),
          bandConditions: undefined,
        }),
      };
      const coolData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices({
            favorable: false,
            solarFluxIndex: 20, // Low score to ensure priority < 20
          }),
          bandConditions: undefined,
        }),
      };

      const { result: hotResult } = renderHook(() => useDashboardCards(hotData));
      const { result: coolResult } = renderHook(() => useDashboardCards(coolData));

      expect(hotResult.current[0].hotness).toBe('hot');
      expect(coolResult.current[0].hotness).toBe('cool');
    });
  });

  describe('bandConditionsConfig', () => {
    it('should return only solar-indices when no bandConditions', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: createMockSolarIndices(),
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Only solar-indices card should be present
      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
    });

    it('should have correct id, type, size for individual band cards', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M })],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // 1 solar (loading) + 1 band card
      expect(result.current).toHaveLength(2);
      const bandCard = result.current.find((c) => c.id === 'band-BAND_20M');
      expect(bandCard).toMatchObject({
        id: 'band-BAND_20M',
        type: 'band-condition',
        size: '1x1',
      });
    });

    it('should use individual band score as priority', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 100 }),
            createMockBandCondition({ band: FrequencyBand.BAND_40M, score: 80 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // 1 solar (loading) + 2 band cards
      expect(result.current).toHaveLength(3);
      // Each card uses its individual band's score as priority
      const band20m = result.current.find((c) => c.id === 'band-BAND_20M');
      const band40m = result.current.find((c) => c.id === 'band-BAND_40M');
      expect(band20m?.priority).toBe(100);
      expect(band40m?.priority).toBe(80);
    });

    it('should calculate hotness for individual band cards', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 100 }), // hot
            createMockBandCondition({ band: FrequencyBand.BAND_40M, score: 10 }), // cool
          ],
        }),
      };

      const { result } = renderHook(() => useDashboardCards(dashboardData));

      const band20m = result.current.find((c) => c.id === 'band-BAND_20M');
      const band40m = result.current.find((c) => c.id === 'band-BAND_40M');

      expect(band20m?.hotness).toBe('hot');
      expect(band40m?.hotness).toBe('cool');
    });
  });

  describe('combined output', () => {
    it('should return solar card plus individual band cards when all data present', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse(),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // 1 solar + 3 individual band cards
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

    it('should return solar loading plus band cards when solar data missing', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // 1 solar (loading) + 3 individual band cards from default mock
      expect(result.current).toHaveLength(4);
      expect(result.current.find((c) => c.id === 'solar-indices')).toBeDefined();
      expect(result.current.find((c) => c.id === 'band-BAND_80M')).toBeDefined();
      expect(result.current.find((c) => c.id === 'band-BAND_40M')).toBeDefined();
      expect(result.current.find((c) => c.id === 'band-BAND_20M')).toBeDefined();
    });

    it('should return solar loading card when all data missing', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Solar indices always shows with loading state
      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('solar-indices');
      expect(result.current[0].priority).toBe(0);
      expect(Array.isArray(result.current)).toBe(true);
    });
  });
});
