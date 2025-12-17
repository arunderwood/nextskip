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
    // Wait for loading to complete (should be < 10 seconds)
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 10000 });

    // Verify ActivityGrid has cards
    const cards = page.locator('.activity-card');
    await expect(cards.first()).toBeVisible();
    expect(await cards.count()).toBeGreaterThan(0);
  });

  test('shows last update timestamp', async ({ page }) => {
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 10000 });
    await expect(page.locator('.last-update')).toContainText('Updated');
  });

  test('theme toggle button is visible', async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');
    await expect(themeToggle).toBeVisible();
  });

  test('can switch to dark mode', async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');

    // Click the toggle to switch to dark mode
    await themeToggle.click();

    // Verify the document has dark theme applied
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('dark');

    // Verify localStorage was updated
    const storedTheme = await page.evaluate(() => localStorage.getItem('nextskip-theme'));
    expect(storedTheme).toBe('dark');
  });

  test('can switch to light mode', async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');

    // Click once to go to dark
    await themeToggle.click();
    await page.waitForTimeout(100);

    // Click again to go to light
    await themeToggle.click();

    // Verify the document has light theme applied
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('light');

    // Verify localStorage was updated
    const storedTheme = await page.evaluate(() => localStorage.getItem('nextskip-theme'));
    expect(storedTheme).toBe('light');
  });

  test('theme persists after page reload', async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');

    // Switch to dark mode
    await themeToggle.click();

    // Reload the page
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('#outlet > *', { timeout: 10000 });

    // Verify dark theme is still applied
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('dark');
  });
});
