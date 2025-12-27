/**
 * Unit tests for SEO constants.
 */

import { describe, it, expect } from 'vitest';
import { APP_METADATA } from 'Frontend/components/seo/constants';

describe('APP_METADATA', () => {
  it('should have required application properties', () => {
    expect(APP_METADATA.name).toBe('NextSkip');
    expect(APP_METADATA.description).toBeTruthy();
    expect(APP_METADATA.url).toMatch(/^https:\/\//);
    expect(APP_METADATA.applicationCategory).toBeTruthy();
  });

  it('should have a valid HTTPS URL', () => {
    expect(APP_METADATA.url).toBe('https://nextskip.io');
  });

  it('should have web application properties', () => {
    expect(APP_METADATA.operatingSystem).toBe('All');
    expect(APP_METADATA.browserRequirements).toBe('Requires JavaScript');
  });

  it('should have a descriptive description', () => {
    expect(APP_METADATA.description).toContain('amateur radio');
    expect(APP_METADATA.description).toContain('dashboard');
  });
});
