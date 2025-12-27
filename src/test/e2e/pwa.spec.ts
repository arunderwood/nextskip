import { test, expect } from '@playwright/test';

/**
 * E2E tests for PWA (Progressive Web App) support
 *
 * These tests verify that PWA manifest and icons are properly served.
 * Note: PWA functionality is only enabled in production builds.
 * In development mode (bootRun), these tests are skipped.
 *
 * Timeout guidelines:
 * - Simple resource checks: 30s
 */
test.describe('PWA Support', () => {
  test('manifest.webmanifest contains required PWA fields', { timeout: 30_000 }, async ({ page }) => {
    const response = await page.goto('/manifest.webmanifest');

    // Skip test in development mode where PWA is disabled
    if (response?.status() === 404) {
      test.skip(true, 'PWA is disabled in development mode');
      return;
    }

    expect(response?.status()).toBe(200);

    const manifest = await response?.json();

    // Required fields exist with reasonable content
    expect(manifest.name).toBeTruthy();
    expect(manifest.short_name).toBeTruthy();
    expect(manifest.theme_color).toMatch(/^#[0-9a-fA-F]{6}$/);
    expect(manifest.background_color).toMatch(/^#[0-9a-fA-F]{6}$/);
    expect(['standalone', 'fullscreen', 'minimal-ui']).toContain(manifest.display);
    expect(manifest.icons).toBeInstanceOf(Array);
    expect(manifest.icons.length).toBeGreaterThanOrEqual(2);

    // Icons have required properties
    manifest.icons.forEach((icon: { src: string; sizes: string; type: string }) => {
      expect(icon.src).toBeTruthy();
      expect(icon.sizes).toMatch(/^\d+x\d+$/);
      expect(icon.type).toBe('image/png');
    });
  });

  test('PWA icons are accessible', { timeout: 30_000 }, async ({ page }) => {
    // Check if PWA is enabled by trying to access manifest first
    const manifestCheck = await page.goto('/manifest.webmanifest');
    if (manifestCheck?.status() === 404) {
      test.skip(true, 'PWA is disabled in development mode');
      return;
    }

    const icon192 = await page.goto('/icons/pwa-192x192.png');
    expect(icon192?.status()).toBe(200);
    expect(icon192?.headers()['content-type']).toContain('image/png');

    const icon512 = await page.goto('/icons/pwa-512x512.png');
    expect(icon512?.status()).toBe(200);
    expect(icon512?.headers()['content-type']).toContain('image/png');
  });

  test('apple-touch-icon is accessible', { timeout: 30_000 }, async ({ page }) => {
    // Check if PWA is enabled by trying to access manifest first
    const manifestCheck = await page.goto('/manifest.webmanifest');
    if (manifestCheck?.status() === 404) {
      test.skip(true, 'PWA is disabled in development mode');
      return;
    }

    const appleIcon = await page.goto('/icons/apple-touch-icon.png');
    expect(appleIcon?.status()).toBe(200);
    expect(appleIcon?.headers()['content-type']).toContain('image/png');
  });
});
