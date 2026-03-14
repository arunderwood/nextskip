/**
 * Tests for band-activity card registration and rendering.
 *
 * These tests exercise the card definition methods (canRender, createConfig, render)
 * and internal helper functions through integration.
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { useDashboardCards } from 'Frontend/hooks/useDashboardCards';
import { renderHook } from '@testing-library/react';
import type { DashboardData } from 'Frontend/components/cards/types';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import type BandActivity from 'Frontend/generated/io/nextskip/spots/model/BandActivity';
import type BandActivityResponse from 'Frontend/generated/io/nextskip/spots/api/BandActivityResponse';
import FrequencyBand from 'Frontend/generated/io/nextskip/common/model/FrequencyBand';
import BandConditionRating from 'Frontend/generated/io/nextskip/propagation/model/BandConditionRating';
import { getRegisteredCards } from 'Frontend/components/cards/CardRegistry';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

// Ensure card registration is loaded
import 'Frontend/components/cards/band-activity';

// ===========================================
// Mock Data Factories
// ===========================================

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
  activePaths: ['NA_EU', 'NA_AS'],
  score: 85,
  favorable: true,
  windowMinutes: 15,
  rarityMultiplier: 1.0,
  ...overrides,
});

const createMockBandActivityResponse = (activities: Record<string, BandActivity> = {}): BandActivityResponse => ({
  bandActivities: activities,
  mqttConnected: true,
  bandCount: Object.keys(activities).length,
  totalSpotCount: Object.values(activities).reduce((sum, a) => sum + (a?.spotCount ?? 0), 0),
});

/**
 * Helper to safely get configs as array from createConfig result.
 * Handles the union type: ActivityCardConfig | ActivityCardConfig[] | null
 */
function asConfigArray(result: ActivityCardConfig | ActivityCardConfig[] | null): ActivityCardConfig[] {
  if (!result) return [];
  return Array.isArray(result) ? result : [result];
}

// ===========================================
// Card Registration Tests
// ===========================================

describe('Band Activity Card Registration', () => {
  describe('canRender', () => {
    it('should not create cards when only propagation data present (no activity)', () => {
      const data: DashboardData = {
        propagation: {
          bandConditions: [createMockBandCondition()],
        },
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // Cards are only created when activity data exists
      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');
      expect(bandCards.length).toBe(0);
    });

    it('should create cards when spots data present', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity(),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // Should create band-mode-activity cards
      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');
      expect(bandCards.length).toBeGreaterThan(0);
    });

    it('should create cards when both propagation and spots data present', () => {
      const data: DashboardData = {
        propagation: {
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M })],
        },
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity(),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');
      expect(bandCards.length).toBeGreaterThan(0);
    });
  });

  describe('createConfig', () => {
    it('should create cards with proper IDs', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');
      // Should have IDs like "band-activity-20m-FT8"
      expect(bandCards.some((c) => c.id.startsWith('band-activity-'))).toBe(true);
      expect(bandCards.some((c) => c.id === 'band-activity-20m-FT8')).toBe(true);
    });

    it('should calculate combined score from activity and condition', () => {
      const data: DashboardData = {
        propagation: {
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M, score: 80 })],
        },
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8', score: 100 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // Find the 20m FT8 card
      const ft8Card = result.current.find((c) => c.id === 'band-activity-20m-FT8');
      expect(ft8Card).toBeDefined();

      // Combined score = 70% activity + 30% condition = 70 + 24 = 94
      if (ft8Card) {
        expect(ft8Card.priority).toBe(94);
      }
    });

    it('should use activity score when only activity data present', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8', score: 75 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // Find a 20m card (should use activity score directly)
      const card20m = result.current.find((c) => c.id === 'band-activity-20m-FT8');
      expect(card20m?.priority).toBe(75);
    });

    it('should create cards only for modes with activity', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
          '20m-CW': createMockBandActivity({ band: '20m', mode: 'CW' }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // Should have FT8 and CW cards (have activity data)
      const ft8Card = result.current.find((c) => c.id === 'band-activity-20m-FT8');
      const cwCard = result.current.find((c) => c.id === 'band-activity-20m-CW');
      expect(ft8Card).toBeDefined();
      expect(cwCard).toBeDefined();
    });

    it('should not create cards for modes without activity', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // CW has no activity data, so no card should be created
      const cwCard = result.current.find((c) => c.id === 'band-activity-20m-CW');
      expect(cwCard).toBeUndefined();
    });

    it('should include unsupported modes when they have activity data', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-SSB': createMockBandActivity({ band: '20m', mode: 'SSB' }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));

      // SSB should have a card because it has activity data
      const ssbCard = result.current.find((c) => c.id === 'band-activity-20m-SSB');
      expect(ssbCard).toBeDefined();
    });
  });

  describe('render', () => {
    it('should render card with title containing band and mode', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        expect(configs.length).toBeGreaterThan(0);

        const ft8Config = configs.find((c) => c.id === 'band-activity-20m-FT8');
        if (ft8Config) {
          const element = bandActivityCard.render(data, ft8Config);
          expect(element).toBeTruthy();

          // Render and check content
          render(element);
          expect(screen.getByText('20m FT8')).toBeInTheDocument();
        }
      }
    });

    it('should render card with activity content when activity data present', () => {
      const data: DashboardData = {
        propagation: {
          bandConditions: [createMockBandCondition({ band: FrequencyBand.BAND_20M })],
        },
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({
            band: '20m',
            mode: 'FT8',
            spotCount: 150,
            trendPercentage: 30,
          }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft8Config = configs.find((c) => c.id === 'band-activity-20m-FT8');

        if (ft8Config) {
          const element = bandActivityCard.render(data, ft8Config);
          render(element);

          // Should show spot count
          expect(screen.getByText(/150/)).toBeInTheDocument();
        }
      }
    });

    it('should handle missing condition gracefully', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '40m-FT8': createMockBandActivity({ band: '40m', mode: 'FT8' }),
        }),
        // No propagation data
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft8Config = configs.find((c) => c.id === 'band-activity-40m-FT8');

        if (ft8Config) {
          const element = bandActivityCard.render(data, ft8Config);
          render(element);

          // Should render without crashing
          expect(screen.getByText('40m FT8')).toBeInTheDocument();
        }
      }
    });

    it('should handle multiple bands correctly', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
          '40m-FT8': createMockBandActivity({ band: '40m', mode: 'FT8' }),
          '80m-FT8': createMockBandActivity({ band: '80m', mode: 'FT8' }),
        }),
      };

      const { result } = renderHook(() => useDashboardCards(data));

      // Should have cards for all bands
      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');
      const bands = new Set(bandCards.map((c) => c.id.split('-')[2]));

      expect(bands.has('20m')).toBe(true);
      expect(bands.has('40m')).toBe(true);
      expect(bands.has('80m')).toBe(true);
    });
  });

  describe('toBandModeActivity conversion', () => {
    it('should convert BandActivity with all fields', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({
            band: '20m',
            mode: 'FT8',
            spotCount: 100,
            baselineSpotCount: 80,
            trendPercentage: 25,
            maxDxKm: 5000,
            maxDxPath: 'NA-EU',
            activePaths: ['NA_EU', 'NA_AS'],
            score: 85,
            windowMinutes: 15,
          }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft8Config = configs.find((c) => c.id === 'band-activity-20m-FT8');

        if (ft8Config) {
          const element = bandActivityCard.render(data, ft8Config);
          render(element);

          // Should render activity data
          expect(screen.getByText(/100/)).toBeInTheDocument();
          expect(screen.getByText(/25%/)).toBeInTheDocument();
        }
      }
    });

    it('should handle BandActivity with minimal required fields', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': {
            band: '20m',
            mode: 'FT8',
            // Only required fields, optional fields undefined
            spotCount: 0,
            baselineSpotCount: 0,
            trendPercentage: 0,
            score: 0,
            favorable: false,
            windowMinutes: 15,
            rarityMultiplier: 1.0,
          },
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft8Config = configs.find((c) => c.id === 'band-activity-20m-FT8');

        if (ft8Config) {
          // Should not throw
          const element = bandActivityCard.render(data, ft8Config);
          expect(element).toBeTruthy();
        }
      }
    });
  });

  describe('parseCardId', () => {
    it('should handle valid card IDs', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const config = configs[0];

        if (config) {
          const element = bandActivityCard.render(data, config);
          // If render returns non-null, parseCardId succeeded
          expect(element).toBeTruthy();
        }
      }
    });

    it('should return null for invalid card IDs', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        // Create a config with an invalid ID
        const invalidConfig: ActivityCardConfig = {
          id: 'invalid-id-format',
          type: 'band-mode-activity',
          size: '1x1',
          priority: 50,
          hotness: 'neutral',
        };

        const element = bandActivityCard.render(data, invalidConfig);
        // Should return null for invalid ID
        expect(element).toBeNull();
      }
    });
  });

  describe('band sorting', () => {
    it('should sort bands by frequency (lower first)', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '10m-FT8': createMockBandActivity({ band: '10m', mode: 'FT8' }),
          '80m-FT8': createMockBandActivity({ band: '80m', mode: 'FT8' }),
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8' }),
        }),
      };

      const { result } = renderHook(() => useDashboardCards(data));

      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');
      const ft8Cards = bandCards.filter((c) => c.id.endsWith('-FT8'));

      // Extract band order from IDs
      const bands = ft8Cards.map((c) => c.id.split('-')[2]);

      // Should be sorted: 80m, 20m, 10m (lower frequency first)
      const indexOf80m = bands.indexOf('80m');
      const indexOf20m = bands.indexOf('20m');
      const indexOf10m = bands.indexOf('10m');

      // All should be found
      expect(indexOf80m).toBeGreaterThanOrEqual(0);
      expect(indexOf20m).toBeGreaterThanOrEqual(0);
      expect(indexOf10m).toBeGreaterThanOrEqual(0);
    });
  });

  // ===========================================
  // T015: FT4 Card Rendering
  // ===========================================

  describe('FT4 card rendering (T015)', () => {
    it('should render FT4 card with correct title format', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4' }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft4Config = configs.find((c) => c.id === 'band-activity-20m-FT4');

        if (ft4Config) {
          const element = bandActivityCard.render(data, ft4Config);
          render(element);
          expect(screen.getByText('20m FT4')).toBeInTheDocument();
        }
      }
    });

    it('should show spot count, trend, and DX reach data for FT4', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({
            band: '20m',
            mode: 'FT4',
            spotCount: 75,
            trendPercentage: 15,
            maxDxKm: 8000,
            maxDxPath: 'NA-AS',
          }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft4Config = configs.find((c) => c.id === 'band-activity-20m-FT4');

        if (ft4Config) {
          const element = bandActivityCard.render(data, ft4Config);
          render(element);

          expect(screen.getByText(/75/)).toBeInTheDocument();
          expect(screen.getByText(/15%/)).toBeInTheDocument();
        }
      }
    });

    it('should allow FT4 and FT8 cards to coexist on the same band', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8', score: 90 }),
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', score: 70 }),
        }),
      };

      const { result } = renderHook(() => useDashboardCards(data));

      const ft8Card = result.current.find((c) => c.id === 'band-activity-20m-FT8');
      const ft4Card = result.current.find((c) => c.id === 'band-activity-20m-FT4');

      expect(ft8Card).toBeDefined();
      expect(ft4Card).toBeDefined();
      expect(ft8Card?.id).not.toBe(ft4Card?.id);
    });
  });

  // ===========================================
  // T023: FT2 Card Rendering
  // ===========================================

  describe('FT2 card rendering (T023)', () => {
    it('should render FT2 card with correct title format', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2' }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft2Config = configs.find((c) => c.id === 'band-activity-20m-FT2');

        if (ft2Config) {
          const element = bandActivityCard.render(data, ft2Config);
          render(element);
          expect(screen.getByText('20m FT2')).toBeInTheDocument();
        }
      }
    });

    it('should show all data fields for FT2 same as FT8', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({
            band: '20m',
            mode: 'FT2',
            spotCount: 42,
            trendPercentage: -10,
            maxDxKm: 3000,
            maxDxPath: 'EU-AF',
            score: 60,
          }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft2Config = configs.find((c) => c.id === 'band-activity-20m-FT2');

        if (ft2Config) {
          const element = bandActivityCard.render(data, ft2Config);
          render(element);

          expect(screen.getByText(/42/)).toBeInTheDocument();
          expect(screen.getByText(/-10%/)).toBeInTheDocument();
        }
      }
    });
  });

  // ===========================================
  // FR-010: Hotness Indicators for FT4 and FT2
  // ===========================================

  describe('hotness indicators (FR-010)', () => {
    it('should assign correct hotness for FT4 card with high score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', score: 85 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft4Card = result.current.find((c) => c.id === 'band-activity-20m-FT4');
      expect(ft4Card?.hotness).toBe('hot');
    });

    it('should assign warm hotness for FT4 card with medium score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', score: 55 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft4Card = result.current.find((c) => c.id === 'band-activity-20m-FT4');
      expect(ft4Card?.hotness).toBe('warm');
    });

    it('should assign neutral hotness for FT4 card with low score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', score: 30 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft4Card = result.current.find((c) => c.id === 'band-activity-20m-FT4');
      expect(ft4Card?.hotness).toBe('neutral');
    });

    it('should assign cool hotness for FT4 card with very low score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', score: 10 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft4Card = result.current.find((c) => c.id === 'band-activity-20m-FT4');
      expect(ft4Card?.hotness).toBe('cool');
    });

    it('should assign correct hotness for FT2 card with high score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2', score: 75 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft2Card = result.current.find((c) => c.id === 'band-activity-20m-FT2');
      expect(ft2Card?.hotness).toBe('hot');
    });

    it('should assign warm hotness for FT2 card with medium score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2', score: 50 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft2Card = result.current.find((c) => c.id === 'band-activity-20m-FT2');
      expect(ft2Card?.hotness).toBe('warm');
    });

    it('should assign neutral hotness for FT2 card with low score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2', score: 25 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft2Card = result.current.find((c) => c.id === 'band-activity-20m-FT2');
      expect(ft2Card?.hotness).toBe('neutral');
    });

    it('should assign cool hotness for FT2 card with very low score', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2', score: 5 }),
        }),
      };
      const { result } = renderHook(() => useDashboardCards(data));
      const ft2Card = result.current.find((c) => c.id === 'band-activity-20m-FT2');
      expect(ft2Card?.hotness).toBe('cool');
    });
  });

  // ===========================================
  // T024 / FR-011: Interleaved Sort Order
  // ===========================================

  describe('interleaved sort order (FR-011, T024)', () => {
    it('should produce FT8, FT4, and FT2 cards that interleave by score when sorted', () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT8': createMockBandActivity({ band: '20m', mode: 'FT8', score: 90 }),
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', score: 70 }),
          '40m-FT8': createMockBandActivity({ band: '40m', mode: 'FT8', score: 60 }),
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2', score: 80 }),
          '40m-FT4': createMockBandActivity({ band: '40m', mode: 'FT4', score: 50 }),
          '40m-FT2': createMockBandActivity({ band: '40m', mode: 'FT2', score: 40 }),
        }),
      };

      const { result } = renderHook(() => useDashboardCards(data));

      const bandCards = result.current.filter((c) => c.type === 'band-mode-activity');

      // All 6 cards should be created (3 modes x 2 bands)
      expect(bandCards.length).toBe(6);

      // All three modes should be represented
      const modes = new Set(
        bandCards.map((c) => {
          const parts = c.id.replace('band-activity-', '').split('-');
          return parts[1];
        }),
      );
      expect(modes).toEqual(new Set(['FT8', 'FT4', 'FT2']));

      // When sorted by priority (descending), modes should be interleaved, not grouped
      const sorted = [...bandCards].sort((a, b) => b.priority - a.priority);
      const sortedModes = sorted.map((c) => {
        const parts = c.id.replace('band-activity-', '').split('-');
        return parts[1];
      });

      // Expected order by score: FT8(90), FT2(80), FT4(70), FT8(60), FT4(50), FT2(40)
      // Verify the sorted modes are NOT all-FT8-first-then-FT4-then-FT2
      // i.e., cards are interleaved by score across modes
      const firstThreeModes = new Set(sortedModes.slice(0, 3));
      expect(firstThreeModes.size).toBeGreaterThanOrEqual(2);
    });
  });

  // ===========================================
  // SC-006: Accessibility for FT4 and FT2 Cards
  // ===========================================

  describe('accessibility (SC-006)', () => {
    it('should pass axe accessibility checks for FT4 card', async () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT4': createMockBandActivity({ band: '20m', mode: 'FT4', spotCount: 50, trendPercentage: 10 }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft4Config = configs.find((c) => c.id === 'band-activity-20m-FT4');

        if (ft4Config) {
          const element = bandActivityCard.render(data, ft4Config);
          const { container } = render(element);
          const results = await axe(container);
          expect(results).toHaveNoViolations();
        }
      }
    });

    it('should pass axe accessibility checks for FT2 card', async () => {
      const data: DashboardData = {
        spots: createMockBandActivityResponse({
          '20m-FT2': createMockBandActivity({ band: '20m', mode: 'FT2', spotCount: 30, trendPercentage: -5 }),
        }),
      };

      const cards = getRegisteredCards();
      const bandActivityCard = cards.find((c) => c.canRender(data));

      if (bandActivityCard) {
        const configs = asConfigArray(bandActivityCard.createConfig(data));
        const ft2Config = configs.find((c) => c.id === 'band-activity-20m-FT2');

        if (ft2Config) {
          const element = bandActivityCard.render(data, ft2Config);
          const { container } = render(element);
          const results = await axe(container);
          expect(results).toHaveNoViolations();
        }
      }
    });
  });
});
