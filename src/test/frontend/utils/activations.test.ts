import { describe, it, expect } from 'vitest';
import { formatFrequency, formatTimeSince } from 'Frontend/utils/activations';

describe('activations utilities', () => {
  describe('formatFrequency', () => {
    it('should format frequencies correctly', () => {
      expect(formatFrequency(14250)).toBe('14.250 MHz');
      expect(formatFrequency(7074)).toBe('7.074 MHz');
      expect(formatFrequency(3573)).toBe('3.573 MHz');
      expect(formatFrequency(21074)).toBe('21.074 MHz');
    });

    it('should handle whole MHz values', () => {
      expect(formatFrequency(14000)).toBe('14.000 MHz');
      expect(formatFrequency(7000)).toBe('7.000 MHz');
    });

    it('should handle single digit MHz', () => {
      expect(formatFrequency(1840)).toBe('1.840 MHz');
      expect(formatFrequency(3500)).toBe('3.500 MHz');
    });

    it('should handle very high frequencies', () => {
      expect(formatFrequency(144000)).toBe('144.000 MHz');
      expect(formatFrequency(432000)).toBe('432.000 MHz');
    });

    it('should handle zero', () => {
      expect(formatFrequency(0)).toBe('Unknown');
    });

    it('should handle undefined', () => {
      expect(formatFrequency(undefined)).toBe('Unknown');
    });

    it('should handle null', () => {
      expect(formatFrequency(null as any)).toBe('Unknown');
    });
  });

  describe('formatTimeSince', () => {
    it('should format recent times as "Just now"', () => {
      const now = new Date();
      const thirtySecondsAgo = new Date(now.getTime() - 30000).toISOString();
      expect(formatTimeSince(thirtySecondsAgo)).toBe('Just now');
    });

    it('should format 1 minute ago', () => {
      const now = new Date();
      const oneMinuteAgo = new Date(now.getTime() - 60 * 1000).toISOString();
      expect(formatTimeSince(oneMinuteAgo)).toBe('1 min ago');
    });

    it('should format minutes ago', () => {
      const now = new Date();
      const fiveMinutesAgo = new Date(now.getTime() - 5 * 60 * 1000).toISOString();
      expect(formatTimeSince(fiveMinutesAgo)).toBe('5 min ago');

      const fiftyNineMinutesAgo = new Date(now.getTime() - 59 * 60 * 1000).toISOString();
      expect(formatTimeSince(fiftyNineMinutesAgo)).toBe('59 min ago');
    });

    it('should format 1 hour ago', () => {
      const now = new Date();
      const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000).toISOString();
      expect(formatTimeSince(oneHourAgo)).toBe('1 hour ago');
    });

    it('should format hours ago', () => {
      const now = new Date();
      const twoHoursAgo = new Date(now.getTime() - 2 * 60 * 60 * 1000).toISOString();
      expect(formatTimeSince(twoHoursAgo)).toBe('2 hours ago');

      const twentyThreeHoursAgo = new Date(now.getTime() - 23 * 60 * 60 * 1000).toISOString();
      expect(formatTimeSince(twentyThreeHoursAgo)).toBe('23 hours ago');
    });

    it('should format many hours ago (no day conversion)', () => {
      const now = new Date();
      const threeDaysInHours = new Date(now.getTime() - 72 * 60 * 60 * 1000).toISOString();
      expect(formatTimeSince(threeDaysInHours)).toBe('72 hours ago');
    });

    it('should handle undefined', () => {
      expect(formatTimeSince(undefined)).toBe('Unknown');
    });

    it('should handle invalid date strings', () => {
      // Invalid dates result in NaN calculations
      expect(formatTimeSince('invalid')).toBe('NaN hours ago');
    });

    it('should handle empty string', () => {
      expect(formatTimeSince('')).toBe('Unknown');
    });
  });
});
