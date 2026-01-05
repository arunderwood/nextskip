import { describe, it, expect } from 'vitest';
import { normalizeBandToString, normalizeBandToEnum, bandsMatch, compareBands } from 'Frontend/utils/bandMapping';

describe('bandMapping utilities', () => {
  describe('normalizeBandToString', () => {
    it('should convert BAND_XXM format to string', () => {
      expect(normalizeBandToString('BAND_20M')).toBe('20m');
      expect(normalizeBandToString('BAND_40M')).toBe('40m');
      expect(normalizeBandToString('BAND_160M')).toBe('160m');
      expect(normalizeBandToString('BAND_6M')).toBe('6m');
    });

    it('should handle already normalized string format', () => {
      expect(normalizeBandToString('20m')).toBe('20m');
      expect(normalizeBandToString('40m')).toBe('40m');
    });

    it('should handle uppercase string format', () => {
      expect(normalizeBandToString('20M')).toBe('20m');
      expect(normalizeBandToString('40M')).toBe('40m');
    });

    it('should handle empty or undefined input', () => {
      expect(normalizeBandToString('')).toBe('');
      expect(normalizeBandToString(undefined)).toBe('');
      expect(normalizeBandToString(null)).toBe('');
    });
  });

  describe('normalizeBandToEnum', () => {
    it('should convert string format to BAND_XXM format', () => {
      expect(normalizeBandToEnum('20m')).toBe('BAND_20M');
      expect(normalizeBandToEnum('40m')).toBe('BAND_40M');
      expect(normalizeBandToEnum('160m')).toBe('BAND_160M');
    });

    it('should handle already normalized enum format', () => {
      expect(normalizeBandToEnum('BAND_20M')).toBe('BAND_20M');
      expect(normalizeBandToEnum('BAND_40M')).toBe('BAND_40M');
    });

    it('should handle mixed case string format', () => {
      expect(normalizeBandToEnum('20M')).toBe('BAND_20M');
    });

    it('should handle empty or undefined input', () => {
      expect(normalizeBandToEnum('')).toBe('');
      expect(normalizeBandToEnum(undefined)).toBe('');
      expect(normalizeBandToEnum(null)).toBe('');
    });
  });

  describe('bandsMatch', () => {
    it('should match identical strings', () => {
      expect(bandsMatch('20m', '20m')).toBe(true);
      expect(bandsMatch('BAND_20M', 'BAND_20M')).toBe(true);
    });

    it('should match equivalent formats', () => {
      expect(bandsMatch('20m', 'BAND_20M')).toBe(true);
      expect(bandsMatch('BAND_20M', '20m')).toBe(true);
      expect(bandsMatch('40m', 'BAND_40M')).toBe(true);
    });

    it('should not match different bands', () => {
      expect(bandsMatch('20m', '40m')).toBe(false);
      expect(bandsMatch('BAND_20M', 'BAND_40M')).toBe(false);
      expect(bandsMatch('20m', 'BAND_40M')).toBe(false);
    });

    it('should be case insensitive', () => {
      expect(bandsMatch('20m', '20M')).toBe(true);
      expect(bandsMatch('band_20m', 'BAND_20M')).toBe(true);
    });
  });

  describe('compareBands', () => {
    it('should sort bands by frequency (160m first, 6m last)', () => {
      const bands = ['20m', '160m', '6m', '40m', '80m'];
      const sorted = [...bands].sort(compareBands);
      expect(sorted).toEqual(['160m', '80m', '40m', '20m', '6m']);
    });

    it('should handle BAND_XXM format', () => {
      const bands = ['BAND_20M', 'BAND_160M', 'BAND_6M'];
      const sorted = [...bands].sort(compareBands);
      expect(sorted).toEqual(['BAND_160M', 'BAND_20M', 'BAND_6M']);
    });

    it('should handle mixed formats', () => {
      const bands = ['20m', 'BAND_160M', '6m'];
      const sorted = [...bands].sort(compareBands);
      expect(sorted).toEqual(['BAND_160M', '20m', '6m']);
    });

    it('should place unknown bands at beginning (index -1)', () => {
      // Unknown bands get indexOf() = -1, so they sort before known bands
      const bands = ['20m', 'unknown', '40m'];
      const sorted = [...bands].sort(compareBands);
      expect(sorted).toEqual(['unknown', '40m', '20m']);
    });
  });
});
