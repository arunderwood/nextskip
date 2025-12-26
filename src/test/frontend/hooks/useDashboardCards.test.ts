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
    it('should return empty array', () => {
      const { result } = renderHook(() => useDashboardCards({} as DashboardData));
      expect(result.current).toEqual([]);
    });
  });

  describe('solarInput calculation', () => {
    it('should return UNKNOWN rating when solarIndices undefined', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Should return empty array when no solar indices
      expect(result.current).toEqual([]);
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
    it('should return UNKNOWN when bandConditions empty', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toEqual([]);
    });

    it('should calculate average score correctly', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ score: 60 }),
            createMockBandCondition({ score: 80 }),
            createMockBandCondition({ score: 100 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('band-conditions');
      // Average score = (60 + 80 + 100) / 3 = 80
      // This contributes 35% * 80 = 28 points to priority
      expect(result.current[0].priority).toBeGreaterThanOrEqual(25);
    });

    it('should set favorable=true when majority favorable', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ favorable: true, score: 80 }),
            createMockBandCondition({ favorable: true, score: 75 }),
            createMockBandCondition({ favorable: false, score: 50 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      // Majority favorable (2/3) should add 40 points to priority
      expect(result.current[0].priority).toBeGreaterThanOrEqual(60);
    });

    it('should set favorable=false when minority favorable', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ favorable: true, score: 80 }),
            createMockBandCondition({ favorable: false, score: 50 }),
            createMockBandCondition({ favorable: false, score: 40 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      // Minority favorable (1/3) should NOT add 40 points
      expect(result.current[0].priority).toBeLessThan(60);
    });

    it('should select GOOD as best rating when present', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ rating: BandConditionRating.GOOD, score: 80 }),
            createMockBandCondition({ rating: BandConditionRating.FAIR, score: 60 }),
            createMockBandCondition({ rating: BandConditionRating.POOR, score: 30 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      // GOOD rating adds 20 points, so priority should reflect this
      expect(result.current[0].priority).toBeGreaterThanOrEqual(40);
    });

    it('should select FAIR as best rating when no GOOD', () => {
      const dataWithFair: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ rating: BandConditionRating.FAIR, score: 60, favorable: true }),
            createMockBandCondition({ rating: BandConditionRating.POOR, score: 30, favorable: true }),
          ],
        }),
      };
      const dataWithPoor: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ rating: BandConditionRating.POOR, score: 60, favorable: true }),
            createMockBandCondition({ rating: BandConditionRating.POOR, score: 30, favorable: true }),
          ],
        }),
      };

      const { result: fairResult } = renderHook(() => useDashboardCards(dataWithFair));
      const { result: poorResult } = renderHook(() => useDashboardCards(dataWithPoor));

      // FAIR rating (12 points) should give higher priority than POOR rating (4 points)
      expect(fairResult.current[0].priority).toBeGreaterThan(poorResult.current[0].priority);
    });

    it('should handle undefined values in band conditions', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ score: 80 }),
            createMockBandCondition({ score: undefined, favorable: true }),
            createMockBandCondition({ score: 60 }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('band-conditions');
      // Should not crash and should calculate from valid values
      expect(result.current[0].priority).toBeGreaterThanOrEqual(0);
    });
  });

  describe('solarIndicesConfig', () => {
    it('should return null config when no solarIndices', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [createMockBandCondition()],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      // Only band-conditions card should be present
      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('band-conditions');
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
        size: 'standard',
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
    it('should return null config when no bandConditions', () => {
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

    it('should have correct id, type, size=tall', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [createMockBandCondition()],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0]).toMatchObject({
        id: 'band-conditions',
        type: 'band-conditions',
        size: 'tall',
      });
    });

    it('should calculate priority correctly', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ favorable: true, score: 100, rating: BandConditionRating.GOOD }),
            createMockBandCondition({ favorable: true, score: 100, rating: BandConditionRating.GOOD }),
          ],
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      // favorable(40) + avg score 100(35) + GOOD rating(20) = 95
      expect(result.current[0].priority).toBeGreaterThanOrEqual(90);
    });

    it('should calculate hotness correctly', () => {
      const hotData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ favorable: true, score: 100, rating: BandConditionRating.GOOD }),
            createMockBandCondition({ favorable: true, score: 100, rating: BandConditionRating.GOOD }),
          ],
        }),
      };
      const coolData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: [
            createMockBandCondition({ favorable: false, score: 20, rating: BandConditionRating.POOR }),
            createMockBandCondition({ favorable: false, score: 10, rating: BandConditionRating.POOR }),
          ],
        }),
      };

      const { result: hotResult } = renderHook(() => useDashboardCards(hotData));
      const { result: coolResult } = renderHook(() => useDashboardCards(coolData));

      expect(hotResult.current[0].hotness).toBe('hot');
      expect(coolResult.current[0].hotness).toBe('cool');
    });
  });

  describe('combined output', () => {
    it('should return both cards when all data present', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse(),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(2);
      expect(result.current[0].id).toBe('solar-indices');
      expect(result.current[1].id).toBe('band-conditions');
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

    it('should return only band card when solar missing', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toHaveLength(1);
      expect(result.current[0].id).toBe('band-conditions');
    });

    it('should filter null configs correctly', () => {
      const dashboardData: DashboardData = {
        propagation: createMockPropagationResponse({
          solarIndices: undefined,
          bandConditions: undefined,
        }),
      };
      const { result } = renderHook(() => useDashboardCards(dashboardData));

      expect(result.current).toEqual([]);
      expect(Array.isArray(result.current)).toBe(true);
    });
  });
});
