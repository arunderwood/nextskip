/**
 * Unit tests for priority calculation algorithm - happy path documentation
 *
 * Invariant tests are covered by usePriorityCalculation.property.test.ts
 * These tests document expected behavior for common use cases.
 *
 * Priority weights:
 * - Favorable flag: 40 points
 * - Score: 35 points max (0-100 normalized)
 * - Rating: 20 points max (GOOD=20, FAIR=12, POOR=6, UNKNOWN=0)
 * - Recency: 5 points max (decays over 60 minutes)
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

const { SCORE_BOUNDS, PRIORITY_WEIGHTS, HOTNESS_THRESHOLDS } = TEST_CONSTANTS;

describe('calculatePriority', () => {
  it('should return max priority (100) with all optimal inputs', () => {
    const priority = calculatePriority({
      favorable: true,
      score: 100,
      rating: 'GOOD',
      lastUpdated: new Date(),
    });

    expect(priority).toBe(SCORE_BOUNDS.MAX);
  });

  it('should return min priority (0) with all worst inputs', () => {
    const priority = calculatePriority({
      favorable: false,
      score: 0,
      rating: 'UNKNOWN',
      lastUpdated: new Date(0), // Old timestamp
    });

    expect(priority).toBe(SCORE_BOUNDS.MIN);
  });

  it('should return only favorable weight when no other inputs', () => {
    const priority = calculatePriority({ favorable: true });

    expect(priority).toBe(PRIORITY_WEIGHTS.FAVORABLE);
  });

  it('should handle empty input gracefully', () => {
    const priority = calculatePriority({ favorable: false });

    expect(priority).toBe(SCORE_BOUNDS.MIN);
  });
});

describe('priorityToHotness', () => {
  it('should return "hot" for high priority (70+)', () => {
    expect(priorityToHotness(HOTNESS_THRESHOLDS.HOT)).toBe('hot');
    expect(priorityToHotness(100)).toBe('hot');
  });

  it('should return "warm" for medium-high priority (45-69)', () => {
    expect(priorityToHotness(HOTNESS_THRESHOLDS.WARM)).toBe('warm');
    expect(priorityToHotness(69)).toBe('warm');
  });

  it('should return "neutral" for medium priority (20-44)', () => {
    expect(priorityToHotness(HOTNESS_THRESHOLDS.NEUTRAL)).toBe('neutral');
    expect(priorityToHotness(44)).toBe('neutral');
  });

  it('should return "cool" for low priority (0-19)', () => {
    expect(priorityToHotness(0)).toBe('cool');
    expect(priorityToHotness(19)).toBe('cool');
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
});
