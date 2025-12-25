import { describe, it, expect } from 'vitest';
import {
  getRatingClass,
  formatBandName,
  getBandDescription,
  sortBandConditions,
  BAND_ORDER,
} from 'Frontend/utils/bandConditions';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import FrequencyBand from 'Frontend/generated/io/nextskip/common/model/FrequencyBand';
import BandConditionRating from 'Frontend/generated/io/nextskip/propagation/model/BandConditionRating';

describe('bandConditions utilities', () => {
  describe('getRatingClass', () => {
    it('should return rating-good for GOOD rating', () => {
      expect(getRatingClass('GOOD')).toBe('rating-good');
    });

    it('should return rating-fair for FAIR rating', () => {
      expect(getRatingClass('FAIR')).toBe('rating-fair');
    });

    it('should return rating-poor for POOR rating', () => {
      expect(getRatingClass('POOR')).toBe('rating-poor');
    });

    it('should be case-insensitive', () => {
      expect(getRatingClass('good')).toBe('rating-good');
      expect(getRatingClass('Fair')).toBe('rating-fair');
      expect(getRatingClass('poor')).toBe('rating-poor');
    });

    it('should return rating-unknown for unknown rating', () => {
      expect(getRatingClass('UNKNOWN')).toBe('rating-unknown');
      expect(getRatingClass('')).toBe('rating-unknown');
    });

    it('should handle undefined/null input', () => {
      expect(getRatingClass(undefined as any)).toBe('rating-unknown');
      expect(getRatingClass(null as any)).toBe('rating-unknown');
    });
  });

  describe('formatBandName', () => {
    it('should remove BAND_ prefix from band names', () => {
      expect(formatBandName('BAND_160M')).toBe('160M');
      expect(formatBandName('BAND_80M')).toBe('80M');
      expect(formatBandName('BAND_40M')).toBe('40M');
      expect(formatBandName('BAND_30M')).toBe('30M');
      expect(formatBandName('BAND_20M')).toBe('20M');
      expect(formatBandName('BAND_17M')).toBe('17M');
      expect(formatBandName('BAND_15M')).toBe('15M');
      expect(formatBandName('BAND_12M')).toBe('12M');
      expect(formatBandName('BAND_10M')).toBe('10M');
      expect(formatBandName('BAND_6M')).toBe('6M');
    });

    it('should return Unknown for empty or undefined', () => {
      expect(formatBandName('')).toBe('Unknown');
      expect(formatBandName(undefined as any)).toBe('Unknown');
    });
  });

  describe('getBandDescription', () => {
    it('should return descriptions for all standard bands', () => {
      expect(getBandDescription('BAND_160M')).toBe('Long distance, nighttime');
      expect(getBandDescription('BAND_80M')).toBe('Regional to DX, night');
      expect(getBandDescription('BAND_40M')).toBe('All-around workhorse');
      expect(getBandDescription('BAND_30M')).toBe('Digital modes, quiet');
      expect(getBandDescription('BAND_20M')).toBe('DX powerhouse');
      expect(getBandDescription('BAND_17M')).toBe('Underutilized gem');
      expect(getBandDescription('BAND_15M')).toBe('DX when conditions support');
      expect(getBandDescription('BAND_12M')).toBe('Daytime DX');
      expect(getBandDescription('BAND_10M')).toBe('Solar cycle dependent');
      expect(getBandDescription('BAND_6M')).toBe('Magic band');
    });

    it('should return empty string for unrecognized bands', () => {
      expect(getBandDescription('unknown')).toBe('');
      expect(getBandDescription('')).toBe('');
    });
  });

  describe('BAND_ORDER', () => {
    it('should define order for all standard bands', () => {
      expect(BAND_ORDER).toEqual([
        'BAND_160M',
        'BAND_80M',
        'BAND_40M',
        'BAND_30M',
        'BAND_20M',
        'BAND_17M',
        'BAND_15M',
        'BAND_12M',
        'BAND_10M',
        'BAND_6M',
      ]);
    });
  });

  describe('sortBandConditions', () => {
    const createCondition = (band: FrequencyBand): BandCondition => ({
      band,
      rating: BandConditionRating.GOOD,
      score: 80,
      favorable: true,
      confidence: 90,
    });

    it('should sort bands in correct order', () => {
      const conditions = [
        createCondition(FrequencyBand.BAND_10M),
        createCondition(FrequencyBand.BAND_160M),
        createCondition(FrequencyBand.BAND_40M),
      ];

      const sorted = sortBandConditions(conditions);

      expect(sorted[0].band).toBe('BAND_160M');
      expect(sorted[1].band).toBe('BAND_40M');
      expect(sorted[2].band).toBe('BAND_10M');
    });

    it('should handle all bands in correct order', () => {
      const conditions = [
        createCondition(FrequencyBand.BAND_10M),
        createCondition(FrequencyBand.BAND_12M),
        createCondition(FrequencyBand.BAND_15M),
        createCondition(FrequencyBand.BAND_17M),
        createCondition(FrequencyBand.BAND_20M),
        createCondition(FrequencyBand.BAND_30M),
        createCondition(FrequencyBand.BAND_40M),
        createCondition(FrequencyBand.BAND_80M),
        createCondition(FrequencyBand.BAND_160M),
        createCondition(FrequencyBand.BAND_6M),
      ];

      const sorted = sortBandConditions(conditions);

      expect(sorted.map((c) => c.band)).toEqual([
        'BAND_160M',
        'BAND_80M',
        'BAND_40M',
        'BAND_30M',
        'BAND_20M',
        'BAND_17M',
        'BAND_15M',
        'BAND_12M',
        'BAND_10M',
        'BAND_6M',
      ]);
    });

    it('should place unknown bands at the beginning', () => {
      // Unknown bands get indexOf() = -1, so they sort first
      // Cast to test function handling of unknown band
      const unknownCondition = {
        band: 'unknown' as FrequencyBand,
        rating: BandConditionRating.GOOD,
        score: 80,
        favorable: true,
        confidence: 90,
      };
      const conditions = [
        createCondition(FrequencyBand.BAND_10M),
        unknownCondition,
        createCondition(FrequencyBand.BAND_160M),
      ];

      const sorted = sortBandConditions(conditions);

      expect(sorted[0].band).toBe('unknown');
      expect(sorted[1].band).toBe('BAND_160M');
      expect(sorted[2].band).toBe('BAND_10M');
    });

    it('should handle empty array', () => {
      const sorted = sortBandConditions([]);
      expect(sorted).toEqual([]);
    });

    it('should not mutate original array', () => {
      // Cast to test function behavior with non-standard band names
      const cond1 = {
        band: '10m' as FrequencyBand,
        rating: BandConditionRating.GOOD,
        score: 80,
        favorable: true,
        confidence: 90,
      };
      const cond2 = {
        band: '80m-40m' as FrequencyBand,
        rating: BandConditionRating.GOOD,
        score: 80,
        favorable: true,
        confidence: 90,
      };
      const conditions = [cond1, cond2];
      const original = [...conditions];

      sortBandConditions(conditions);

      expect(conditions).toEqual(original);
    });
  });
});
