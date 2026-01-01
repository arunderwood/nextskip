/**
 * Unit tests for priority calculation algorithm
 *
 * Tests the weighted priority scoring system using invariant-based assertions:
 * - Favorable flag: 40% weight (invariant: favorable=true always adds points)
 * - Score: 35% weight (invariant: higher score -> higher priority)
 * - Rating: 20% weight (invariant: GOOD >= FAIR >= POOR >= UNKNOWN)
 * - Recency: 5% weight (invariant: newer -> higher priority)
 *
 * @see TEST_CONSTANTS for threshold and weight values
 */

import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import {
  calculatePriority,
  priorityToHotness,
  usePriorityCalculation,
} from 'Frontend/components/activity/usePriorityCalculation';
import { TEST_CONSTANTS } from '../../testConstants';

const { SCORE_BOUNDS, RATING_ORDER, PRIORITY_WEIGHTS, HOTNESS_THRESHOLDS, RECENCY_DECAY_MINUTES } = TEST_CONSTANTS;

describe('calculatePriority', () => {
  // ==========================================================================
  // Score Bounds Invariant Tests
  // ==========================================================================

  describe('score bounds invariant', () => {
    it('should always return priority within bounds', () => {
      // Test various input combinations
      const testCases = [
        { favorable: true, score: 100, rating: 'GOOD' as const, lastUpdated: new Date() },
        { favorable: false, score: 0, rating: 'UNKNOWN' as const },
        { favorable: true, score: 50, rating: 'FAIR' as const },
        { favorable: false },
      ];

      for (const input of testCases) {
        const priority = calculatePriority(input);
        expect(priority).toBeGreaterThanOrEqual(SCORE_BOUNDS.MIN);
        expect(priority).toBeLessThanOrEqual(SCORE_BOUNDS.MAX);
      }
    });

    it('should return integer values', () => {
      const priority = calculatePriority({
        favorable: false,
        score: 33, // Non-round value to test rounding
      });
      expect(Number.isInteger(priority)).toBe(true);
    });
  });

  // ==========================================================================
  // Favorable Flag Invariant Tests
  // ==========================================================================

  describe('favorable flag invariant', () => {
    it('should always add points when favorable is true', () => {
      const withFavorable = calculatePriority({ favorable: true });
      const withoutFavorable = calculatePriority({ favorable: false });

      expect(withFavorable).toBeGreaterThan(withoutFavorable);
      expect(withFavorable).toBe(PRIORITY_WEIGHTS.FAVORABLE);
      expect(withoutFavorable).toBe(0);
    });

    it('should increase priority by favorable weight for any base conditions', () => {
      const baseConditions = [{ score: 50 }, { rating: 'FAIR' as const }, { score: 100, rating: 'GOOD' as const }];

      for (const base of baseConditions) {
        const withFavorable = calculatePriority({ ...base, favorable: true });
        const withoutFavorable = calculatePriority({ ...base, favorable: false });

        expect(withFavorable - withoutFavorable).toBe(PRIORITY_WEIGHTS.FAVORABLE);
      }
    });
  });

  // ==========================================================================
  // Score Monotonicity Invariant Tests
  // ==========================================================================

  describe('score monotonicity invariant', () => {
    it('should produce higher or equal priority for higher scores', () => {
      const scores = [0, 25, 50, 75, 100];

      for (let i = 1; i < scores.length; i++) {
        const lowerScore = calculatePriority({ favorable: false, score: scores[i - 1] });
        const higherScore = calculatePriority({ favorable: false, score: scores[i] });

        expect(higherScore).toBeGreaterThanOrEqual(lowerScore);
      }
    });

    it('should clamp scores to valid range', () => {
      const normalMax = calculatePriority({ favorable: false, score: 100 });
      const overMax = calculatePriority({ favorable: false, score: 150 });

      expect(overMax).toBe(normalMax);

      const normalMin = calculatePriority({ favorable: false, score: 0 });
      const underMin = calculatePriority({ favorable: false, score: -50 });

      expect(underMin).toBe(normalMin);
    });

    it('should max score contribute exactly SCORE weight', () => {
      const maxScoreOnly = calculatePriority({ favorable: false, score: 100 });
      expect(maxScoreOnly).toBe(PRIORITY_WEIGHTS.SCORE);
    });
  });

  // ==========================================================================
  // Rating Ordering Invariant Tests
  // ==========================================================================

  describe('rating ordering invariant', () => {
    it('should maintain GOOD >= FAIR >= POOR >= UNKNOWN ordering', () => {
      const priorities = RATING_ORDER.map((rating) => calculatePriority({ favorable: false, rating }));

      // Verify monotonic non-increasing
      for (let i = 1; i < priorities.length; i++) {
        expect(priorities[i]).toBeLessThanOrEqual(priorities[i - 1]);
      }
    });

    it('should maintain rating ordering at any favorable flag value', () => {
      for (const favorable of [true, false]) {
        const priorities = RATING_ORDER.map((rating) => calculatePriority({ favorable, rating }));

        for (let i = 1; i < priorities.length; i++) {
          expect(priorities[i]).toBeLessThanOrEqual(priorities[i - 1]);
        }
      }
    });

    it('should have GOOD rating contribute exactly RATING weight', () => {
      const goodOnly = calculatePriority({ favorable: false, rating: 'GOOD' });
      expect(goodOnly).toBe(PRIORITY_WEIGHTS.RATING);
    });
  });

  // ==========================================================================
  // Recency Decay Invariant Tests
  // ==========================================================================

  describe('recency decay invariant', () => {
    it('should produce higher or equal priority for newer timestamps', () => {
      const now = new Date();
      const tenMinsAgo = new Date(Date.now() - 10 * 60 * 1000);
      const thirtyMinsAgo = new Date(Date.now() - 30 * 60 * 1000);
      const sixtyMinsAgo = new Date(Date.now() - RECENCY_DECAY_MINUTES * 60 * 1000);

      const timestamps = [sixtyMinsAgo, thirtyMinsAgo, tenMinsAgo, now];

      for (let i = 1; i < timestamps.length; i++) {
        const older = calculatePriority({ favorable: false, lastUpdated: timestamps[i - 1] });
        const newer = calculatePriority({ favorable: false, lastUpdated: timestamps[i] });

        expect(newer).toBeGreaterThanOrEqual(older);
      }
    });

    it('should decay to zero at or beyond decay threshold', () => {
      const beyondDecay = new Date(Date.now() - (RECENCY_DECAY_MINUTES + 10) * 60 * 1000);
      const priority = calculatePriority({ favorable: false, lastUpdated: beyondDecay });

      expect(priority).toBe(0);
    });

    it('should have current timestamp contribute exactly RECENCY weight', () => {
      const currentOnly = calculatePriority({ favorable: false, lastUpdated: new Date() });
      expect(currentOnly).toBe(PRIORITY_WEIGHTS.RECENCY);
    });
  });

  // ==========================================================================
  // Combined Input Invariant Tests
  // ==========================================================================

  describe('combined input invariants', () => {
    it('should reach maximum priority with all optimal inputs', () => {
      const maxPriority = calculatePriority({
        favorable: true,
        score: 100,
        rating: 'GOOD',
        lastUpdated: new Date(),
      });

      expect(maxPriority).toBe(SCORE_BOUNDS.MAX);
    });

    it('should reach minimum priority with all worst inputs', () => {
      const minPriority = calculatePriority({
        favorable: false,
        score: 0,
        rating: 'UNKNOWN',
        lastUpdated: new Date(Date.now() - RECENCY_DECAY_MINUTES * 60 * 1000),
      });

      expect(minPriority).toBe(SCORE_BOUNDS.MIN);
    });

    it('should handle missing optional fields gracefully', () => {
      const priority = calculatePriority({
        favorable: true,
        // No score, rating, or recency
      });

      expect(priority).toBe(PRIORITY_WEIGHTS.FAVORABLE);
    });

    it('should handle undefined values without errors', () => {
      const priorities = [
        calculatePriority({ favorable: true, score: undefined, rating: 'GOOD' }),
        calculatePriority({ favorable: true, score: 100, rating: undefined }),
        calculatePriority({ favorable: false }),
      ];

      for (const p of priorities) {
        expect(p).toBeGreaterThanOrEqual(SCORE_BOUNDS.MIN);
        expect(p).toBeLessThanOrEqual(SCORE_BOUNDS.MAX);
      }
    });
  });
});

describe('priorityToHotness', () => {
  // ==========================================================================
  // Hotness Threshold Invariant Tests
  // ==========================================================================

  describe('threshold boundaries', () => {
    it('should return "hot" for priority >= HOT threshold', () => {
      expect(priorityToHotness(HOTNESS_THRESHOLDS.HOT)).toBe('hot');
      expect(priorityToHotness(SCORE_BOUNDS.MAX)).toBe('hot');
    });

    it('should return "warm" for priority in [WARM, HOT) range', () => {
      expect(priorityToHotness(HOTNESS_THRESHOLDS.WARM)).toBe('warm');
      expect(priorityToHotness(HOTNESS_THRESHOLDS.HOT - 1)).toBe('warm');
    });

    it('should return "neutral" for priority in [NEUTRAL, WARM) range', () => {
      expect(priorityToHotness(HOTNESS_THRESHOLDS.NEUTRAL)).toBe('neutral');
      expect(priorityToHotness(HOTNESS_THRESHOLDS.WARM - 1)).toBe('neutral');
    });

    it('should return "cool" for priority < NEUTRAL threshold', () => {
      expect(priorityToHotness(SCORE_BOUNDS.MIN)).toBe('cool');
      expect(priorityToHotness(HOTNESS_THRESHOLDS.NEUTRAL - 1)).toBe('cool');
    });
  });

  describe('monotonicity invariant', () => {
    it('should have hotness levels ordered by increasing priority', () => {
      // cool < neutral < warm < hot
      const hotnessOrder = ['cool', 'neutral', 'warm', 'hot'];
      const priorities = [0, 25, 50, 85]; // Representative values for each level

      for (let i = 0; i < priorities.length; i++) {
        expect(priorityToHotness(priorities[i])).toBe(hotnessOrder[i]);
      }
    });
  });
});

describe('usePriorityCalculation hook', () => {
  it('should calculate priority and hotness together', () => {
    const { result } = renderHook(() =>
      usePriorityCalculation({
        favorable: true,
        score: 100,
        rating: 'GOOD',
        lastUpdated: new Date(),
      }),
    );

    expect(result.current.priority).toBe(SCORE_BOUNDS.MAX);
    expect(result.current.hotness).toBe('hot');
  });

  it('should return cool hotness for low priority inputs', () => {
    const { result } = renderHook(() =>
      usePriorityCalculation({
        favorable: false,
        score: 15,
        rating: 'POOR',
      }),
    );

    expect(result.current.priority).toBeLessThan(HOTNESS_THRESHOLDS.NEUTRAL);
    expect(result.current.hotness).toBe('cool');
  });

  it('should handle empty input', () => {
    const { result } = renderHook(() => usePriorityCalculation({ favorable: false }));

    expect(result.current.priority).toBe(SCORE_BOUNDS.MIN);
    expect(result.current.hotness).toBe('cool');
  });

  it('should maintain priority-hotness consistency', () => {
    const testInputs = [
      { favorable: true, score: 100, rating: 'GOOD' as const, lastUpdated: new Date() },
      { favorable: true, score: 50, rating: 'FAIR' as const },
      { favorable: false, score: 30, rating: 'POOR' as const },
      { favorable: false },
    ];

    for (const input of testInputs) {
      const { result } = renderHook(() => usePriorityCalculation(input));
      const { priority, hotness } = result.current;

      // Verify hotness is consistent with priority
      if (priority >= HOTNESS_THRESHOLDS.HOT) {
        expect(hotness).toBe('hot');
      } else if (priority >= HOTNESS_THRESHOLDS.WARM) {
        expect(hotness).toBe('warm');
      } else if (priority >= HOTNESS_THRESHOLDS.NEUTRAL) {
        expect(hotness).toBe('neutral');
      } else {
        expect(hotness).toBe('cool');
      }
    }
  });
});
