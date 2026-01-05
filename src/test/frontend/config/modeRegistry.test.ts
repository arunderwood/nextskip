import { describe, it, expect } from 'vitest';
import {
  getModeConfig,
  getSupportedModes,
  getAllModes,
  isModeSupported,
  getModeDisplayName,
} from 'Frontend/config/modeRegistry';

describe('modeRegistry', () => {
  describe('getModeConfig', () => {
    it('should return config for FT8', () => {
      const config = getModeConfig('FT8');
      expect(config).toBeDefined();
      expect(config?.mode).toBe('FT8');
      expect(config?.isSupported).toBe(true);
    });

    it('should return config for CW', () => {
      const config = getModeConfig('CW');
      expect(config).toBeDefined();
      expect(config?.mode).toBe('CW');
      expect(config?.isSupported).toBe(true);
    });

    it('should return config for unsupported modes', () => {
      const ssbConfig = getModeConfig('SSB');
      expect(ssbConfig).toBeDefined();
      expect(ssbConfig?.mode).toBe('SSB');
      expect(ssbConfig?.isSupported).toBe(false);

      const ft4Config = getModeConfig('FT4');
      expect(ft4Config).toBeDefined();
      expect(ft4Config?.mode).toBe('FT4');
      expect(ft4Config?.isSupported).toBe(false);
    });

    it('should return undefined for unknown mode', () => {
      const config = getModeConfig('UNKNOWN_MODE');
      expect(config).toBeUndefined();
    });

    it('should handle undefined input', () => {
      const config = getModeConfig(undefined);
      expect(config).toBeUndefined();
    });

    it('should be case sensitive (matches backend Mode enum)', () => {
      // Mode identifiers match backend enum exactly (uppercase)
      expect(getModeConfig('ft8')).toBeUndefined();
      expect(getModeConfig('FT8')).toBeDefined();
    });
  });

  describe('getSupportedModes', () => {
    it('should return only supported modes', () => {
      const supported = getSupportedModes();
      expect(supported.every((m) => m.isSupported)).toBe(true);
    });

    it('should include FT8 and CW', () => {
      const supported = getSupportedModes();
      const modes = supported.map((m) => m.mode);
      expect(modes).toContain('FT8');
      expect(modes).toContain('CW');
    });

    it('should not include SSB or FT4', () => {
      const supported = getSupportedModes();
      const modes = supported.map((m) => m.mode);
      expect(modes).not.toContain('SSB');
      expect(modes).not.toContain('FT4');
    });
  });

  describe('getAllModes', () => {
    it('should return all modes (supported and unsupported)', () => {
      const all = getAllModes();
      expect(all.length).toBeGreaterThan(getSupportedModes().length);
    });

    it('should include both supported and unsupported modes', () => {
      const all = getAllModes();
      const hasSupported = all.some((m) => m.isSupported);
      const hasUnsupported = all.some((m) => !m.isSupported);
      expect(hasSupported).toBe(true);
      expect(hasUnsupported).toBe(true);
    });

    it('should include common modes', () => {
      const all = getAllModes();
      const modes = all.map((m) => m.mode);
      expect(modes).toContain('FT8');
      expect(modes).toContain('CW');
      expect(modes).toContain('SSB');
      expect(modes).toContain('FT4');
    });
  });

  describe('ModeConfig structure', () => {
    it('should have required fields', () => {
      const config = getModeConfig('FT8');
      expect(config).toHaveProperty('mode');
      expect(config).toHaveProperty('isSupported');
    });

    it('should have optional displayName', () => {
      const config = getModeConfig('FT8');
      // displayName is optional, so we just check it exists or is undefined
      expect(config?.displayName === undefined || typeof config?.displayName === 'string').toBe(true);
    });
  });

  describe('isModeSupported', () => {
    it('should return true for supported modes', () => {
      expect(isModeSupported('FT8')).toBe(true);
      expect(isModeSupported('CW')).toBe(true);
    });

    it('should return false for unsupported modes', () => {
      expect(isModeSupported('SSB')).toBe(false);
      expect(isModeSupported('FT4')).toBe(false);
    });

    it('should return false for unknown modes', () => {
      expect(isModeSupported('UNKNOWN_MODE')).toBe(false);
    });

    it('should return false for undefined input', () => {
      expect(isModeSupported(undefined)).toBe(false);
    });
  });

  describe('getModeDisplayName', () => {
    it('should return mode value when no displayName override', () => {
      expect(getModeDisplayName('FT8')).toBe('FT8');
      expect(getModeDisplayName('CW')).toBe('CW');
      expect(getModeDisplayName('SSB')).toBe('SSB');
    });

    it('should return empty string for undefined input', () => {
      expect(getModeDisplayName(undefined)).toBe('');
    });

    it('should return mode value for unknown modes', () => {
      // Unknown modes return the input string as-is
      expect(getModeDisplayName('UNKNOWN_MODE')).toBe('UNKNOWN_MODE');
    });
  });
});
