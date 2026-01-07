import { describe, it, expect } from 'vitest';
import {
  calculateCombinedScore,
  isCombinedFavorable,
  getWeights,
  ACTIVITY_WEIGHT,
  CONDITION_WEIGHT,
} from 'Frontend/utils/combinedScore';

describe('combinedScore utilities', () => {
  describe('calculateCombinedScore', () => {
    it('should calculate weighted score when both scores present', () => {
      const result = calculateCombinedScore({
        activityScore: 100,
        conditionScore: 100,
      });

      // 100 * 0.7 + 100 * 0.3 = 100
      expect(result).toBe(100);
    });

    it('should apply 70/30 weighting correctly', () => {
      const result = calculateCombinedScore({
        activityScore: 100,
        conditionScore: 0,
      });

      // 100 * 0.7 + 0 * 0.3 = 70
      expect(result).toBe(70);
    });

    it('should apply condition weight when only condition present', () => {
      const result = calculateCombinedScore({
        activityScore: 0,
        conditionScore: 100,
      });

      // 0 * 0.7 + 100 * 0.3 = 30
      expect(result).toBe(30);
    });

    it('should use activity score only when condition is undefined', () => {
      const result = calculateCombinedScore({
        activityScore: 80,
        conditionScore: undefined,
      });

      expect(result).toBe(80);
    });

    it('should use condition score only when activity is undefined', () => {
      const result = calculateCombinedScore({
        activityScore: undefined,
        conditionScore: 60,
      });

      expect(result).toBe(60);
    });

    it('should return 0 when both scores are undefined', () => {
      const result = calculateCombinedScore({
        activityScore: undefined,
        conditionScore: undefined,
      });

      expect(result).toBe(0);
    });

    it('should return 0 when input is empty object', () => {
      const result = calculateCombinedScore({});
      expect(result).toBe(0);
    });

    it('should handle zero scores correctly', () => {
      const result = calculateCombinedScore({
        activityScore: 0,
        conditionScore: 0,
      });

      expect(result).toBe(0);
    });

    it('should round to nearest integer', () => {
      const result = calculateCombinedScore({
        activityScore: 75,
        conditionScore: 50,
      });

      // 75 * 0.7 + 50 * 0.3 = 52.5 + 15 = 67.5 -> 68
      expect(result).toBe(68);
    });

    it('should clamp result between 0 and 100', () => {
      // Even with out-of-range inputs, result should be clamped
      const highResult = calculateCombinedScore({
        activityScore: 150,
        conditionScore: 150,
      });
      expect(highResult).toBeLessThanOrEqual(100);

      const lowResult = calculateCombinedScore({
        activityScore: -10,
        conditionScore: -10,
      });
      expect(lowResult).toBeGreaterThanOrEqual(0);
    });
  });

  describe('weight constants', () => {
    it('should have ACTIVITY_WEIGHT of 0.7', () => {
      expect(ACTIVITY_WEIGHT).toBe(0.7);
    });

    it('should have CONDITION_WEIGHT of 0.3', () => {
      expect(CONDITION_WEIGHT).toBe(0.3);
    });

    it('should have weights that sum to 1.0', () => {
      expect(ACTIVITY_WEIGHT + CONDITION_WEIGHT).toBe(1.0);
    });
  });

  describe('isCombinedFavorable', () => {
    it('should return true when activity is favorable', () => {
      expect(isCombinedFavorable(true, false)).toBe(true);
      expect(isCombinedFavorable(true, undefined)).toBe(true);
    });

    it('should return true when condition is favorable', () => {
      expect(isCombinedFavorable(false, true)).toBe(true);
      expect(isCombinedFavorable(undefined, true)).toBe(true);
    });

    it('should return true when both are favorable', () => {
      expect(isCombinedFavorable(true, true)).toBe(true);
    });

    it('should return false when neither is favorable', () => {
      expect(isCombinedFavorable(false, false)).toBe(false);
      expect(isCombinedFavorable(undefined, undefined)).toBe(false);
      expect(isCombinedFavorable(undefined, false)).toBe(false);
      expect(isCombinedFavorable(false, undefined)).toBe(false);
    });
  });

  describe('getWeights', () => {
    it('should return activity and condition weights', () => {
      const weights = getWeights();
      expect(weights.activity).toBe(0.7);
      expect(weights.condition).toBe(0.3);
    });

    it('should return weights that sum to 1.0', () => {
      const weights = getWeights();
      expect(weights.activity + weights.condition).toBe(1.0);
    });
  });
});
