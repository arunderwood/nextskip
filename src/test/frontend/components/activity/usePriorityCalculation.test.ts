/**
 * Unit tests for priority calculation algorithm
 *
 * Tests the weighted priority scoring system:
 * - Favorable flag: 40% weight
 * - Score: 35% weight (0-100 scale)
 * - Rating: 20% weight (GOOD/FAIR/POOR/UNKNOWN)
 * - Recency: 5% weight (decays over 60 minutes)
 */

import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import {
  calculatePriority,
  priorityToHotness,
  usePriorityCalculation,
} from "Frontend/components/activity/usePriorityCalculation";
import type { PriorityInput } from "Frontend/components/activity";

describe('calculatePriority', () => {
  describe('favorable flag (40% weight)', () => {
    it('should add 40 points when favorable is true', () => {
      const priority = calculatePriority({ favorable: true });
      expect(priority).toBe(40);
    });

    it('should add 0 points when favorable is false', () => {
      const priority = calculatePriority({ favorable: false });
      expect(priority).toBe(0);
    });
  });

  describe('score (35% weight)', () => {
    it('should add full 35 points for score of 100', () => {
      const priority = calculatePriority({ favorable: false, score: 100 });
      expect(priority).toBe(35);
    });

    it('should add 17.5 points for score of 50', () => {
      const priority = calculatePriority({ favorable: false, score: 50 });
      expect(priority).toBe(18); // Rounded
    });

    it('should add 0 points for score of 0', () => {
      const priority = calculatePriority({ favorable: false, score: 0 });
      expect(priority).toBe(0);
    });

    it('should clamp scores above 100', () => {
      const priority = calculatePriority({ favorable: false, score: 150 });
      expect(priority).toBe(35); // Same as score: 100
    });

    it('should clamp scores below 0', () => {
      const priority = calculatePriority({ favorable: false, score: -50 });
      expect(priority).toBe(0);
    });
  });

  describe('rating (20% weight)', () => {
    it('should add 20 points for GOOD rating', () => {
      const priority = calculatePriority({ favorable: false, rating: 'GOOD' });
      expect(priority).toBe(20); // 100/100 * 20 = 20
    });

    it('should add 12 points for FAIR rating', () => {
      const priority = calculatePriority({ favorable: false, rating: 'FAIR' });
      expect(priority).toBe(12); // 60/100 * 20 = 12
    });

    it('should add 4 points for POOR rating', () => {
      const priority = calculatePriority({ favorable: false, rating: 'POOR' });
      expect(priority).toBe(4); // 20/100 * 20 = 4
    });

    it('should add 0 points for UNKNOWN rating', () => {
      const priority = calculatePriority({
        favorable: false,
        rating: 'UNKNOWN',
      });
      expect(priority).toBe(0);
    });
  });

  describe('recency (5% weight)', () => {
    it('should add full 5 points for timestamp now', () => {
      const now = new Date();
      const priority = calculatePriority({ favorable: false, lastUpdated: now });
      expect(priority).toBe(5);
    });

    it('should add ~2.5 points for timestamp 30 minutes ago', () => {
      const thirtyMinsAgo = new Date(Date.now() - 30 * 60 * 1000);
      const priority = calculatePriority({
        favorable: false,
        lastUpdated: thirtyMinsAgo,
      });
      expect(priority).toBeGreaterThanOrEqual(2);
      expect(priority).toBeLessThanOrEqual(3);
    });

    it('should add 0 points for timestamp 60+ minutes ago', () => {
      const sixtyMinsAgo = new Date(Date.now() - 60 * 60 * 1000);
      const priority = calculatePriority({
        favorable: false,
        lastUpdated: sixtyMinsAgo,
      });
      expect(priority).toBe(0);
    });
  });

  describe('combined inputs', () => {
    it('should calculate 100 priority for perfect conditions', () => {
      const priority = calculatePriority({
        favorable: true, // 40
        score: 100, // 35
        rating: 'GOOD', // 20
        lastUpdated: new Date(), // 5
      });
      expect(priority).toBe(100);
    });

    it('should calculate ~70 priority for moderate conditions', () => {
      const priority = calculatePriority({
        favorable: true, // 40
        score: 50, // 17.5
        rating: 'FAIR', // 12 (60/100 * 20)
        // No recency
      });
      // 40 + 17.5 + 12 = 69.5, rounded to 70
      expect(priority).toBe(70);
    });

    it('should calculate 4 priority for worst conditions', () => {
      const priority = calculatePriority({
        favorable: false, // 0
        score: 0, // 0
        rating: 'POOR', // 4 (20/100 * 20)
        lastUpdated: new Date(Date.now() - 120 * 60 * 1000), // 0 (2 hours old)
      });
      expect(priority).toBe(4); // Only POOR rating contributes
    });

    it('should handle missing optional fields gracefully', () => {
      const priority = calculatePriority({
        favorable: true,
        // No score, rating, or recency
      });
      expect(priority).toBe(40); // Just the favorable flag
    });
  });

  describe('edge cases', () => {
    it('should handle undefined score', () => {
      const priority = calculatePriority({
        favorable: true,
        score: undefined,
        rating: 'GOOD',
      });
      expect(priority).toBe(60); // 40 + 20
    });

    it('should handle undefined rating', () => {
      const priority = calculatePriority({
        favorable: true,
        score: 100,
        rating: undefined,
      });
      expect(priority).toBe(75); // 40 + 35
    });

    it('should round to nearest integer', () => {
      const priority = calculatePriority({
        favorable: false,
        score: 33, // Should give ~11.55 points
      });
      expect(Number.isInteger(priority)).toBe(true);
    });
  });
});

describe('priorityToHotness', () => {
  it('should return "hot" for priority 70-100', () => {
    expect(priorityToHotness(70)).toBe('hot');
    expect(priorityToHotness(85)).toBe('hot');
    expect(priorityToHotness(100)).toBe('hot');
  });

  it('should return "warm" for priority 45-69', () => {
    expect(priorityToHotness(45)).toBe('warm');
    expect(priorityToHotness(57)).toBe('warm');
    expect(priorityToHotness(69)).toBe('warm');
  });

  it('should return "neutral" for priority 20-44', () => {
    expect(priorityToHotness(20)).toBe('neutral');
    expect(priorityToHotness(32)).toBe('neutral');
    expect(priorityToHotness(44)).toBe('neutral');
  });

  it('should return "cool" for priority 0-19', () => {
    expect(priorityToHotness(0)).toBe('cool');
    expect(priorityToHotness(10)).toBe('cool');
    expect(priorityToHotness(19)).toBe('cool');
  });

  it('should handle boundary values correctly', () => {
    expect(priorityToHotness(69)).toBe('warm');
    expect(priorityToHotness(70)).toBe('hot');
    expect(priorityToHotness(44)).toBe('neutral');
    expect(priorityToHotness(45)).toBe('warm');
    expect(priorityToHotness(19)).toBe('cool');
    expect(priorityToHotness(20)).toBe('neutral');
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
      })
    );

    expect(result.current.priority).toBe(100);
    expect(result.current.hotness).toBe('hot');
  });

  it('should return cool hotness for low priority inputs', () => {
    const { result } = renderHook(() =>
      usePriorityCalculation({
        favorable: false,
        score: 15,
        rating: 'POOR',
      })
    );

    expect(result.current.priority).toBeLessThan(20);
    expect(result.current.hotness).toBe('cool');
  });

  it('should handle empty input', () => {
    const { result } = renderHook(() =>
      usePriorityCalculation({ favorable: false })
    );

    expect(result.current.priority).toBe(0);
    expect(result.current.hotness).toBe('cool');
  });
});
