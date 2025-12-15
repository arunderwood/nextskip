import { test, expect } from '@playwright/test';

/**
 * E2E tests for the NextSkip Dashboard
 *
 * These tests verify that the dashboard loads correctly and displays the expected content.
 */
test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    // Wait for Vaadin to fully initialize by checking for the outlet div to have content
    await page.waitForSelector('#outlet > *', { timeout: 10000 });
  });

  test('page loads successfully', async ({ page }) => {
    // Give Vaadin time to set the title dynamically if needed
    await page.waitForFunction(() => document.title !== '', { timeout: 5000 });
    await expect(page).toHaveTitle(/NextSkip/i);
  });

  test('header displays correctly', async ({ page }) => {
    await expect(page.locator('.dashboard-title')).toContainText('NextSkip');
    await expect(page.locator('.dashboard-subtitle')).toContainText('HF Propagation Dashboard');
  });

  test('dashboard cards render after loading', async ({ page }) => {
    // Wait for loading to complete
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });

    // Verify BentoGrid has cards
    const cards = page.locator('.bento-card');
    await expect(cards.first()).toBeVisible();
    expect(await cards.count()).toBeGreaterThan(0);
  });

  test('shows last update timestamp', async ({ page }) => {
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });
    await expect(page.locator('.last-update')).toContainText('Updated');
  });
});
