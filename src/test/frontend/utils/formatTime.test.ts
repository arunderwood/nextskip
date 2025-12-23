import { describe, it, expect } from 'vitest';
import { formatTimeRemaining, formatTimeSince } from 'Frontend/utils/formatTime';

describe('formatTime utilities', () => {
  describe('formatTimeRemaining', () => {
    it('should format minutes correctly', () => {
      expect(formatTimeRemaining(30)).toBe('0m');
      expect(formatTimeRemaining(60)).toBe('1m');
      expect(formatTimeRemaining(300)).toBe('5m');
      expect(formatTimeRemaining(3540)).toBe('59m');
    });

    it('should format hours and minutes correctly', () => {
      expect(formatTimeRemaining(3600)).toBe('1h 0m');
      expect(formatTimeRemaining(3660)).toBe('1h 1m');
      expect(formatTimeRemaining(7200)).toBe('2h 0m');
      expect(formatTimeRemaining(7320)).toBe('2h 2m');
      expect(formatTimeRemaining(86340)).toBe('23h 59m');
    });

    it('should format days and hours correctly', () => {
      expect(formatTimeRemaining(86400)).toBe('1d 0h');
      expect(formatTimeRemaining(90000)).toBe('1d 1h');
      expect(formatTimeRemaining(172800)).toBe('2d 0h');
      expect(formatTimeRemaining(176400)).toBe('2d 1h');
    });

    it('should handle negative values as absolute', () => {
      expect(formatTimeRemaining(-60)).toBe('1m');
      expect(formatTimeRemaining(-3600)).toBe('1h 0m');
      expect(formatTimeRemaining(-86400)).toBe('1d 0h');
    });

    it('should handle zero', () => {
      expect(formatTimeRemaining(0)).toBe('0m');
    });

    it('should handle undefined', () => {
      expect(formatTimeRemaining(undefined)).toBe('');
    });

    it('should handle null', () => {
      expect(formatTimeRemaining(null as any)).toBe('');
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
