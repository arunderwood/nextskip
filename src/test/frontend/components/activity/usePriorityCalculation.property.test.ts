/**
 * Property-based tests for priority calculation using fast-check.
 *
 * These tests verify invariants that must hold across all possible inputs:
 * - Score bounds: Priority always in [0, 100]
 * - Monotonicity: Better inputs produce higher or equal priorities
 * - Favorable flag: favorable=true never decreases priority
 * - Rating ordering: GOOD >= FAIR >= POOR >= UNKNOWN
 *
 * @see TEST_CONSTANTS for threshold and weight values
 */

import { describe, it, expect } from 'vitest';
import fc from 'fast-check';
import { calculatePriority, priorityToHotness } from 'Frontend/components/activity/usePriorityCalculation';
import { TEST_CONSTANTS } from '../../testConstants';

const { SCORE_BOUNDS, RATING_ORDER, PRIORITY_WEIGHTS, HOTNESS_THRESHOLDS, RECENCY_DECAY_MINUTES } = TEST_CONSTANTS;

// =============================================================================
// Custom Arbitraries
// =============================================================================

/** Arbitrary for valid rating values */
const ratingArbitrary = fc.constantFrom(...RATING_ORDER);

/** Arbitrary for valid scores (0-100) */
const scoreArbitrary = fc.integer({ min: 0, max: 100 });

/** Arbitrary for timestamps within the last hour */
const recentTimestampArbitrary = fc
  .integer({ min: 0, max: RECENCY_DECAY_MINUTES })
  .map((minutesAgo) => new Date(Date.now() - minutesAgo * 60 * 1000));

/** Arbitrary for complete priority input */
const priorityInputArbitrary = fc.record({
  favorable: fc.boolean(),
  score: fc.option(scoreArbitrary, { nil: undefined }),
  rating: fc.option(ratingArbitrary, { nil: undefined }),
  lastUpdated: fc.option(recentTimestampArbitrary, { nil: undefined }),
});

describe('calculatePriority property tests', () => {
  // ===========================================================================
  // Score Bounds Properties
  // ===========================================================================

  describe('score bounds property', () => {
    it('should always return priority in [0, 100] for any valid input', () => {
      fc.assert(
        fc.property(priorityInputArbitrary, (input) => {
          const priority = calculatePriority(input);

          expect(priority).toBeGreaterThanOrEqual(SCORE_BOUNDS.MIN);
          expect(priority).toBeLessThanOrEqual(SCORE_BOUNDS.MAX);
          expect(Number.isInteger(priority)).toBe(true);
        }),
        { numRuns: 100 },
      );
    });

    it('should handle extreme score values without error', () => {
      fc.assert(
        fc.property(fc.integer({ min: -1000, max: 1000 }), fc.boolean(), (score, favorable) => {
          const priority = calculatePriority({ favorable, score });

          expect(priority).toBeGreaterThanOrEqual(SCORE_BOUNDS.MIN);
          expect(priority).toBeLessThanOrEqual(SCORE_BOUNDS.MAX);
        }),
        { numRuns: 50 },
      );
    });
  });

  // ===========================================================================
  // Favorable Flag Property
  // ===========================================================================

  describe('favorable flag property', () => {
    it('should never decrease priority when favorable is true', () => {
      fc.assert(
        fc.property(
          fc.option(scoreArbitrary, { nil: undefined }),
          fc.option(ratingArbitrary, { nil: undefined }),
          fc.option(recentTimestampArbitrary, { nil: undefined }),
          (score, rating, lastUpdated) => {
            const withFavorable = calculatePriority({
              favorable: true,
              score,
              rating,
              lastUpdated,
            });
            const withoutFavorable = calculatePriority({
              favorable: false,
              score,
              rating,
              lastUpdated,
            });

            expect(withFavorable).toBeGreaterThanOrEqual(withoutFavorable);
          },
        ),
        { numRuns: 50 },
      );
    });

    it('should add exactly FAVORABLE weight when favorable changes from false to true', () => {
      fc.assert(
        fc.property(
          fc.option(scoreArbitrary, { nil: undefined }),
          fc.option(ratingArbitrary, { nil: undefined }),
          (score, rating) => {
            const withFavorable = calculatePriority({ favorable: true, score, rating });
            const withoutFavorable = calculatePriority({ favorable: false, score, rating });

            expect(withFavorable - withoutFavorable).toBe(PRIORITY_WEIGHTS.FAVORABLE);
          },
        ),
        { numRuns: 50 },
      );
    });
  });

  // ===========================================================================
  // Rating Ordering Property
  // ===========================================================================

  describe('rating ordering property', () => {
    it('should maintain GOOD >= FAIR >= POOR >= UNKNOWN ordering for any favorable value', () => {
      fc.assert(
        fc.property(fc.boolean(), (favorable) => {
          const priorities = RATING_ORDER.map((rating) => calculatePriority({ favorable, rating }));

          // Verify monotonic non-increasing
          for (let i = 1; i < priorities.length; i++) {
            expect(priorities[i]).toBeLessThanOrEqual(priorities[i - 1]);
          }
        }),
        { numRuns: 10 }, // Just 2 boolean values, but run a few times
      );
    });
  });

  // ===========================================================================
  // Score Contribution Property
  // ===========================================================================

  describe('score contribution property', () => {
    it('should produce higher or equal priority for higher scores', () => {
      fc.assert(
        fc.property(scoreArbitrary, scoreArbitrary, fc.boolean(), (score1, score2, favorable) => {
          const priority1 = calculatePriority({ favorable, score: score1 });
          const priority2 = calculatePriority({ favorable, score: score2 });

          if (score1 > score2) {
            expect(priority1).toBeGreaterThanOrEqual(priority2);
          } else if (score2 > score1) {
            expect(priority2).toBeGreaterThanOrEqual(priority1);
          } else {
            expect(priority1).toBe(priority2);
          }
        }),
        { numRuns: 50 },
      );
    });
  });

  // ===========================================================================
  // Recency Decay Property
  // ===========================================================================

  describe('recency decay property', () => {
    it('should produce higher or equal priority for newer timestamps', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: RECENCY_DECAY_MINUTES }),
          fc.integer({ min: 0, max: RECENCY_DECAY_MINUTES }),
          (olderMinutes, newerMinutes) => {
            const older = new Date(Date.now() - olderMinutes * 60 * 1000);
            const newer = new Date(Date.now() - newerMinutes * 60 * 1000);

            const olderPriority = calculatePriority({ favorable: false, lastUpdated: older });
            const newerPriority = calculatePriority({ favorable: false, lastUpdated: newer });

            if (olderMinutes > newerMinutes) {
              expect(newerPriority).toBeGreaterThanOrEqual(olderPriority);
            } else if (newerMinutes > olderMinutes) {
              expect(olderPriority).toBeGreaterThanOrEqual(newerPriority);
            } else {
              expect(olderPriority).toBe(newerPriority);
            }
          },
        ),
        { numRuns: 50 },
      );
    });
  });
});

describe('priorityToHotness property tests', () => {
  // ===========================================================================
  // Hotness Monotonicity Property
  // ===========================================================================

  describe('hotness monotonicity property', () => {
    it('should produce consistent hotness levels across the priority range', () => {
      fc.assert(
        fc.property(fc.integer({ min: SCORE_BOUNDS.MIN, max: SCORE_BOUNDS.MAX }), (priority) => {
          const hotness = priorityToHotness(priority);

          // Verify hotness is one of the valid values
          expect(['cool', 'neutral', 'warm', 'hot']).toContain(hotness);

          // Verify hotness is consistent with thresholds
          if (priority >= HOTNESS_THRESHOLDS.HOT) {
            expect(hotness).toBe('hot');
          } else if (priority >= HOTNESS_THRESHOLDS.WARM) {
            expect(hotness).toBe('warm');
          } else if (priority >= HOTNESS_THRESHOLDS.NEUTRAL) {
            expect(hotness).toBe('neutral');
          } else {
            expect(hotness).toBe('cool');
          }
        }),
        { numRuns: 100 },
      );
    });

    it('should produce higher or equal hotness for higher priorities', () => {
      const hotnessOrder = { cool: 0, neutral: 1, warm: 2, hot: 3 };

      fc.assert(
        fc.property(
          fc.integer({ min: SCORE_BOUNDS.MIN, max: SCORE_BOUNDS.MAX }),
          fc.integer({ min: SCORE_BOUNDS.MIN, max: SCORE_BOUNDS.MAX }),
          (p1, p2) => {
            const h1 = priorityToHotness(p1);
            const h2 = priorityToHotness(p2);

            if (p1 > p2) {
              expect(hotnessOrder[h1]).toBeGreaterThanOrEqual(hotnessOrder[h2]);
            } else if (p2 > p1) {
              expect(hotnessOrder[h2]).toBeGreaterThanOrEqual(hotnessOrder[h1]);
            }
          },
        ),
        { numRuns: 50 },
      );
    });
  });
});
