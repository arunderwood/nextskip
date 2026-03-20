import { describe, it, expect } from 'vitest';
import { formatTimeRemaining } from 'Frontend/utils/formatTime';

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
});
