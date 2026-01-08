/**
 * Tests for band-specific DX distance thresholds.
 * Focus: Business logic invariants, not implementation details.
 */
import { describe, it, expect } from 'vitest';
import { bandDxThresholds, getThresholdsForBand, defaultDxThresholds } from 'Frontend/utils/bandDxThresholds';

describe('bandDxThresholds', () => {
  describe('threshold ordering invariant', () => {
    it('should have excellent > good > moderate for all bands', () => {
      for (const threshold of bandDxThresholds) {
        expect(threshold.excellentKm).toBeGreaterThan(threshold.goodKm);
        expect(threshold.goodKm).toBeGreaterThan(threshold.moderateKm);
      }
    });

    it('should have positive values for all thresholds', () => {
      for (const threshold of bandDxThresholds) {
        expect(threshold.excellentKm).toBeGreaterThan(0);
        expect(threshold.goodKm).toBeGreaterThan(0);
        expect(threshold.moderateKm).toBeGreaterThan(0);
      }
    });
  });

  describe('propagation characteristics', () => {
    it('should have 20m as highest excellent threshold (workhorse DX band)', () => {
      const threshold20m = bandDxThresholds.find((t) => t.band === '20m');
      const maxExcellent = Math.max(...bandDxThresholds.map((t) => t.excellentKm));
      expect(threshold20m?.excellentKm).toBe(maxExcellent);
    });

    it('should have 2m as lowest excellent threshold (VHF line-of-sight)', () => {
      const threshold2m = bandDxThresholds.find((t) => t.band === '2m');
      const minExcellent = Math.min(...bandDxThresholds.map((t) => t.excellentKm));
      expect(threshold2m?.excellentKm).toBe(minExcellent);
    });
  });

  describe('getThresholdsForBand', () => {
    it('should return threshold for valid band', () => {
      const result = getThresholdsForBand('20m');
      expect(result).toBeDefined();
      expect(result?.band).toBe('20m');
    });

    it('should return undefined for unknown band', () => {
      expect(getThresholdsForBand('999m')).toBeUndefined();
    });

    it('should return undefined for empty string', () => {
      expect(getThresholdsForBand('')).toBeUndefined();
    });

    it('should be case-sensitive (band names are lowercase)', () => {
      expect(getThresholdsForBand('20M')).toBeUndefined();
    });
  });

  describe('defaultDxThresholds', () => {
    it('should have excellent > good > moderate ordering', () => {
      expect(defaultDxThresholds.excellentKm).toBeGreaterThan(defaultDxThresholds.goodKm);
      expect(defaultDxThresholds.goodKm).toBeGreaterThan(defaultDxThresholds.moderateKm);
    });

    it('should have positive threshold values', () => {
      expect(defaultDxThresholds.excellentKm).toBeGreaterThan(0);
      expect(defaultDxThresholds.goodKm).toBeGreaterThan(0);
      expect(defaultDxThresholds.moderateKm).toBeGreaterThan(0);
    });
  });
});
