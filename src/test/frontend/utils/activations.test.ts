import { describe, it, expect } from 'vitest';
import { formatFrequency } from 'Frontend/utils/activations';

/**
 * Tests for activations-specific utilities.
 * Note: formatTimeSince tests are in formatTime.test.ts (canonical location)
 */
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
});
